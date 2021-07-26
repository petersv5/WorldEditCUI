package com.mumfrey.worldeditcui.render.shapes;

import com.mumfrey.worldeditcui.event.listeners.CUIRenderContext;
import com.mumfrey.worldeditcui.render.LineStyle;
import com.mumfrey.worldeditcui.render.RenderStyle;
import com.mumfrey.worldeditcui.util.Vector3;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.Chunk;

public class RenderChunkBoundary extends RenderRegion
{
	private final MinecraftClient mc;
	private final Render3DGrid grid;
	
	public RenderChunkBoundary(RenderStyle boundaryStyle, RenderStyle gridStyle, MinecraftClient minecraft)
	{
		super(boundaryStyle);

		this.mc = minecraft;
		
		this.grid = new Render3DGrid(gridStyle, Vector3.ZERO, Vector3.ZERO);
		this.grid.setSpacing(4.0);
	}
	
	@Override
	public void render(CUIRenderContext ctx)
	{
		double yMin = this.mc.world != null ? this.mc.world.getDimension().getMinimumY() : 0.0;
		double yMax = this.mc.world != null ? this.mc.world.getDimension().getLogicalHeight() - yMin : 256.0;

		long xBlock = MathHelper.floor(ctx.cameraPos().getX());
		long zBlock = MathHelper.floor(ctx.cameraPos().getZ());

		int xChunk = (int)(xBlock >> 4);
		int zChunk = (int)(zBlock >> 4);
		
		double xBase = 0 - (xBlock - (xChunk * 16)) - (ctx.cameraPos().getX() - xBlock);
		double zBase = (0 - (zBlock - (zChunk * 16)) - (ctx.cameraPos().getZ() - zBlock)) + 16;
		
		this.grid.setPosition(new Vector3(xBase - OFFSET, yMin, zBase - 16 - OFFSET), new Vector3(xBase + 16 + OFFSET, yMax, zBase + OFFSET));

		ctx.matrices().push();
		ctx.matrices().translate(0.0, -ctx.cameraPos().getY(), 0.0);
		ctx.flush();
		ctx.applyMatrices();

		ctx.withCameraAt(Vector3.ZERO, this.grid::render);

		this.renderChunkBorder(ctx, yMin, yMax, xBase, zBase);
		
		if (this.mc.world != null)
		{
			this.renderChunkBoundary(ctx, xChunk, zChunk, xBase, zBase);
		}

		ctx.flush();
		ctx.matrices().pop();
		ctx.applyMatrices();
	}

	private void renderChunkBorder(final CUIRenderContext ctx, double yMin, double yMax, double xBase, double zBase)
	{
		final int spacing = 16;
		
		for (LineStyle line : this.style.getLines())
		{
			if (ctx.apply(line, this.style.getRenderType()))
			{
				ctx.color(line)
					.beginLines();

				for (int x = -16; x <= 32; x += spacing)
				{
					for (int z = -16; z <= 32; z += spacing)
					{
						ctx.vertex(xBase + x, yMin, zBase - z)
							.vertex(xBase + x, yMax, zBase - z);
					}
				}
				
				for (double y = yMin; y <= yMax; y += yMax)
				{
					ctx.vertex(xBase, y, zBase)
						.vertex(xBase, y, zBase - 16)
						.vertex(xBase, y, zBase - 16)
						.vertex(xBase + 16, y, zBase - 16)
						.vertex(xBase + 16, y, zBase - 16)
						.vertex(xBase + 16, y, zBase)
						.vertex(xBase + 16, y, zBase)
						.vertex(xBase, y, zBase);
				}

				ctx.endLines();
			}
		}
	}

	private void renderChunkBoundary(final CUIRenderContext ctx, int xChunk, int zChunk, double xBase, double zBase)
	{
		Chunk chunk = this.mc.world.getChunk(xChunk, zChunk);
		Heightmap heightMap = chunk.getHeightmap(Heightmap.Type.WORLD_SURFACE);

		for (LineStyle line : this.style.getLines())
		{
			if (ctx.apply(line, this.style.getRenderType()))
			{
				ctx.beginLines()
						.color(line);

				int[][] lastHeight = { { -1, -1 }, { -1, -1 } };
				for (int i = 0, height = 0; i < 16; i++)
				{
					for (int j = 0; j < 2; j++)
					{
						for (int axis = 0; axis < 2; axis++)
						{
							height = axis == 0 ? heightMap.get(j * 15, i) : heightMap.get(i, j * 15);
							double xPos = axis == 0 ? xBase + (j * 16) : xBase + i;
							double zPos = axis == 0 ? zBase - 16 + i : zBase - 16 + (j * 16);
							if (lastHeight[axis][j] > -1 && height != lastHeight[axis][j])
							{
								ctx.vertex(xPos, lastHeight[axis][j] + OFFSET, zPos)
									.vertex(xPos, height + OFFSET, zPos);
							}
							ctx.vertex(xPos, height + OFFSET, zPos)
								.vertex(xPos + axis, height + OFFSET, zPos + (1 - axis));
							lastHeight[axis][j] = height;
						}
					}
				}

				ctx.endLines();
			}
		}
	}
	
}
