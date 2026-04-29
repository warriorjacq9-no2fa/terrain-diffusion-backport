package com.github.warriorjacq9.terraindiffusionbp;

import com.github.warriorjacq9.terraindiffusionbp.explorer.ExplorerServer;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.LocalTerrainProvider;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.ModelAssetManager;
import com.github.warriorjacq9.terraindiffusionbp.pipeline.PipelineModels;
import com.github.warriorjacq9.terraindiffusionbp.world.TerrainDiffusionBiomeSource;
import com.github.warriorjacq9.terraindiffusionbp.world.TerrainDiffusionChunkGenerator;
import com.github.warriorjacq9.terraindiffusionbp.world.WorldScaleManager;
import com.mojang.brigadier.context.CommandContext;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v1.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.util.registry.Registry;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.LiteralText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static net.minecraft.server.command.CommandManager.literal;

public class TerrainDiffusionMCBackport implements ModInitializer {
    public static final String MOD_ID = "terrain-diffusion-mc";
    private static final Logger LOG = LoggerFactory.getLogger(TerrainDiffusionMCBackport.class);

    @Override
    public void onInitialize() {
        LOG.info("Initializing terrain-diffusion-mc");
        Registry.register(Registry.BIOME_SOURCE, new Identifier(MOD_ID, "terrain_diffusion"), TerrainDiffusionBiomeSource.CODEC);
        Registry.register(Registry.CHUNK_GENERATOR, new Identifier(MOD_ID, "terrain_diffusion"), TerrainDiffusionChunkGenerator.CODEC);

        ModelAssetManager.ensureAssetsReady();
        PipelineModels.load();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> LocalTerrainProvider.clearCache());

        ServerWorldEvents.LOAD.register((server, world) -> {
            if (world.getRegistryKey() == World.OVERWORLD) {
                WorldScaleManager.initializeForWorld(world);
                LocalTerrainProvider.init(world.getSeed());
            }
        });

        ServerLifecycleEvents.SERVER_STOPPING.register(server -> ExplorerServer.stop());

        CommandRegistrationCallback.EVENT.register((dispatcher, dedicated) ->
                dispatcher.register(literal("td-explore").executes(TerrainDiffusionMCBackport::executeExplore))
        );
    }

    private static int executeExplore(CommandContext<ServerCommandSource> ctx) {
        try {
            int port = ExplorerServer.startIfNotRunning();
            String url = "http://localhost:" + port;
            MutableText link = new LiteralText(url);
            Style linkStyle = Style.EMPTY
                .withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
                .withUnderline(true);
            link.setStyle(linkStyle);
            ctx.getSource().sendFeedback(
                    new LiteralText("Terrain Explorer: ").append(link),
                    false);
        } catch (Exception e) {
            LOG.error("Failed to start terrain explorer", e);
            ctx.getSource().sendError(new LiteralText("Failed to start terrain explorer: " + e.getMessage()));
        }
        return 1;
    }
}