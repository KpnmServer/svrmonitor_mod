
package com.github.kpnmserver.svrmonitor_mod.command;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
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
import com.github.kpnmserver.svrmonitor_mod.storage.Config;

public final class SvrMonitorCommand{
	public static LiteralCommandNode<ServerCommandSource> makeConfigCommand(){
		return literal("config")
			.then(literal("enable")
				.then(literal("get").executes((context)->{
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor is " + (Config.INSTANCE.getEnable() ?"enable" :"disable") + " now"), true);
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("set").then(
					RequiredArgumentBuilder.<ServerCommandSource, Boolean>argument("value", BoolArgumentType.bool()).executes((context)->{
					final boolean enable = context.getArgument("value", Boolean.class).booleanValue();
					Config.INSTANCE.setEnable(enable);
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor is " + (Config.INSTANCE.getEnable() ?"enable" :"disable") + " now"), true);
					return Command.SINGLE_SUCCESS;
				})))
			)
			.then(literal("upload_time")
				.then(literal("get").executes((context)->{
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor upload interval is: " + Config.INSTANCE.getUploadTime()), true);
					return Command.SINGLE_SUCCESS;
				}))
				.then(literal("set").then(
					RequiredArgumentBuilder.<ServerCommandSource, Integer>argument("value", IntegerArgumentType.integer(1)).executes((context)->{
					final int upload_time = context.getArgument("value", Integer.class).intValue();
					Config.INSTANCE.setUploadTime(upload_time);
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor upload interval is set to: " + Config.INSTANCE.getUploadTime()), true);
					return Command.SINGLE_SUCCESS;
				})))
			)
			.build();
	}

	public static void register(CommandDispatcher<ServerCommandSource> dispatcher){
		dispatcher.register(literal("svrmonitor")
			.requires((ServerCommandSource source)->{
				return source.hasPermissionLevel(3);
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
			.then(literal("status")
				.executes((context)->{
					final boolean isalive = SvrMonitorMod.INSTANCE.getUploader() != null && SvrMonitorMod.INSTANCE.getUploader().isalive();
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor " + (isalive ?"is" :"isn't") + " alive now"), true);
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(literal("start")
				.executes((context)->{
					if(SvrMonitorMod.INSTANCE.getUploader() != null){
						context.getSource().sendFeedback(SvrMonitorMod.createText(
								"Start uploader ERROR: Uploader already exists"), false);
						return Command.SINGLE_SUCCESS;
					}
					if(Config.INSTANCE.getEnable()){
						context.getSource().sendFeedback(SvrMonitorMod.createText(
							"Starting uploader..."), true);
						try{
							SvrMonitorMod.INSTANCE.startUploader();
						}catch(Exception e){
							context.getSource().sendFeedback(SvrMonitorMod.createText(
								"Start uploader ERROR: " + e.getMessage()), true);
							return Command.SINGLE_SUCCESS;
						}
					}
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor start SUCCESS"), true);
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(literal("stop")
				.executes((context)->{
					if(SvrMonitorMod.INSTANCE.getUploader() == null){
						context.getSource().sendFeedback(SvrMonitorMod.createText(
								"Stop uploader ERROR: Uploader not exists"), false);
						return Command.SINGLE_SUCCESS;
					}
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Stopping uploader..."), true);
					SvrMonitorMod.INSTANCE.stopUploader();
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor stop SUCCESS"), true);
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(literal("restart")
				.executes((context)->{
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Stopping uploader..."), true);
					SvrMonitorMod.INSTANCE.stopUploader();
					if(Config.INSTANCE.getEnable()){
						context.getSource().sendFeedback(SvrMonitorMod.createText(
							"Starting uploader..."), true);
						try{
							SvrMonitorMod.INSTANCE.startUploader();
						}catch(Exception e){
							context.getSource().sendFeedback(SvrMonitorMod.createText(
								"Restart uploader ERROR: " + e.getMessage()), true);
							return Command.SINGLE_SUCCESS;
						}
					}
					context.getSource().sendFeedback(SvrMonitorMod.createText(
						"Server monitor restart SUCCESS"), true);
					return Command.SINGLE_SUCCESS;
				})
			)
			.then(makeConfigCommand())
		);
	}
}
