package com.example.makan.YouAreMySpecial;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class SpecialMaze {

    public static void generateMazeWithAnimation(ServerPlayer player, int size, int wallHeight) {
        MinecraftServer server = player.getServer();
        if (server == null) {
            player.sendSystemMessage(Component.literal("❌ Server not found."));
            return;
        }

        ServerLevel level = player.serverLevel();
        Vec3 eyePos = player.getEyePosition(1.0F);
        Vec3 lookDir = player.getLookAngle().normalize();
        double maxDistance = 512;
        Vec3 endVec = eyePos.add(lookDir.scale(maxDistance));

        HitResult result = level.clip(new ClipContext(
                eyePos, endVec,
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (result.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("❌ No valid block targeted."));
            return;
        }

        Vec3 hitVec = result.getLocation();
        BlockPos center = new BlockPos((int) hitVec.x, (int) hitVec.y, (int) hitVec.z);

        player.sendSystemMessage(Component.literal("🌀 Animating maze generation at: " + center));

        // Maze grid (odd dimensions only so walls + paths alternate)
        if (size % 2 == 0) size++;
        int mazeSize = size;

        boolean[][] maze = generateMaze(mazeSize);

        // Clear area for minotaur in center
        int clearRadius = mazeSize / 4; // quarter of maze size
        for (int x = -clearRadius; x <= clearRadius; x++) {
            for (int z = -clearRadius; z <= clearRadius; z++) {
                if (x * x + z * z <= clearRadius * clearRadius) {
                    int gridX = x + mazeSize / 2;
                    int gridZ = z + mazeSize / 2;
                    if (gridX >= 0 && gridX < mazeSize && gridZ >= 0 && gridZ < mazeSize) {
                        maze[gridX][gridZ] = true; // path (no wall)
                    }
                }
            }
        }

        Map<Integer, List<BlockPlacement>> layers = new TreeMap<>();

        // Build blocks
        for (int x = 0; x < mazeSize; x++) {
            for (int z = 0; z < mazeSize; z++) {
                if (!maze[x][z]) { // wall
                    for (int y = 0; y < wallHeight; y++) {
                        BlockPos pos = center.offset(x - mazeSize / 2, y, z - mazeSize / 2);
                        BlockState state = Blocks.STONE_BRICKS.defaultBlockState();
                        layers.computeIfAbsent(y, k -> new ArrayList<>()).add(new BlockPlacement(pos, state));
                    }
                }
            }
        }

        int totalBlocks = layers.values().stream().mapToInt(List::size).sum();
        player.sendSystemMessage(Component.literal("⏳ Animating " + totalBlocks + " maze blocks..."));

        AtomicInteger currentLayer = new AtomicInteger(0);
        List<Integer> heights = new ArrayList<>(layers.keySet());

        Runnable animateNextLayer = new Runnable() {
            @Override
            public void run() {
                if (currentLayer.get() >= heights.size()) {
                    player.sendSystemMessage(Component.literal("✅ Maze animation complete!"));
                    return;
                }

                int layerY = heights.get(currentLayer.getAndIncrement());
                List<BlockPlacement> placements = layers.get(layerY);

                for (BlockPlacement placement : placements) {
                    level.setBlock(placement.pos, placement.state, 2);
                }

                server.execute(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException ignored) {}
                    server.execute(this);
                });
            }
        };

        server.execute(animateNextLayer);
    }

    // Simple DFS maze generator
    private static boolean[][] generateMaze(int size) {
        boolean[][] grid = new boolean[size][size]; // true = path, false = wall
        for (int x = 0; x < size; x++) {
            Arrays.fill(grid[x], false);
        }

        Random random = new Random();
        int startX = 1;
        int startZ = 1;

        Deque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{startX, startZ});
        grid[startX][startZ] = true;

        int[][] dirs = {{2, 0}, {-2, 0}, {0, 2}, {0, -2}};

        while (!stack.isEmpty()) {
            int[] cell = stack.peek();
            int cx = cell[0];
            int cz = cell[1];

            List<int[]> neighbors = new ArrayList<>();
            for (int[] d : dirs) {
                int nx = cx + d[0];
                int nz = cz + d[1];
                if (nx > 0 && nx < size - 1 && nz > 0 && nz < size - 1 && !grid[nx][nz]) {
                    neighbors.add(new int[]{nx, nz});
                }
            }

            if (!neighbors.isEmpty()) {
                int[] chosen = neighbors.get(random.nextInt(neighbors.size()));
                int nx = chosen[0];
                int nz = chosen[1];

                // Knock down wall between
                grid[cx + (nx - cx) / 2][cz + (nz - cz) / 2] = true;
                grid[nx][nz] = true;
                stack.push(new int[]{nx, nz});
            } else {
                stack.pop();
            }
        }

        return grid;
    }

    public static class BlockPlacement {
        public final BlockPos pos;
        public final BlockState state;

        public BlockPlacement(BlockPos pos, BlockState state) {
            this.pos = pos;
            this.state = state;
        }
    }
}
