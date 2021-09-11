
package com.github.kpnmserver.svrmonitor_mod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import static net.minecraft.server.command.CommandManager.literal;

import com.github.kpnmserver.svrmonitor_mod.SvrMonitorMod;

public final class SvrMonitorCommand{
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
		dispatcher.register(literal("svrmonitor")
			.requires((ServerCommandSource source)->{
				return source.hasPermissionLevel(4);
			})
			.then(literal("reload")
				.executes((context)->{
					context.getSource().sendFeedback(SvrMonitorMod.createText("reloading monitor config..."), true);
					SvrMonitorMod.INSTANCE.onReload();
					context.getSource().sendFeedback(SvrMonitorMod.createText("reload config SUCCESS"), true);
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(literal("save")
				.executes((context)->{
					context.getSource().sendFeedback(SvrMonitorMod.createText("saving monitor config..."), true);
					SvrMonitorMod.INSTANCE.onSave();
					context.getSource().sendFeedback(SvrMonitorMod.createText("save config SUCCESS"), true);
					return Command.SINGLE_SUCCESS;
				})
			)
		);
	}
}
