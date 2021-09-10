
package com.github.kpnmserver.svrmonitor_mod.connect;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.github.kpnmserver.svrmonitor_mod.SvrMonitorMod;
import com.github.kpnmserver.svrmonitor_mod.connect.InfoUploader;
import com.github.kpnmserver.svrmonitor_mod.storage.Config;
import com.github.kpnmserver.svrmonitor_mod.util.JsonUtil;

public final class WSInfoUploader extends WebSocketClient implements InfoUploader{
	private final Timer ping_timer = new Timer("ws-ping-timer", true);
	private volatile boolean running = false;
	private volatile boolean isbroken = false;

	public WSInfoUploader(final String url) throws URISyntaxException {
		this(new URI(url));
	}

	public WSInfoUploader(final URI uri){
		super(uri);
		try{
			if(!super.connectBlocking(5L, TimeUnit.SECONDS)){
				SvrMonitorMod.LOGGER.error("Can not connect to remote server: " + uri.toString());
				return;
			}
		}catch(InterruptedException e){}
		this.ping_timer.scheduleAtFixedRate(new TimerTask(){
			@Override
			public void run(){
				if(WSInfoUploader.this.running){
					WSInfoUploader.this.sendMessage(JsonUtil.asMap("status", "ping"));
				}
			}
		}, 0L, 1000L);
	}

	public boolean isRunning(){
		return this.running;
	}

	public boolean isBroken(){
		return this.isbroken;
	}

	@Override
	public void onOpen(ServerHandshake shake){
		this.init();
	}

	@Override
	public void onClose(int paramInt, String paramString, boolean paramBoolean){
		if(paramInt == 1006){
			this.isbroken = true;
		}
		this.running = false;
		this.ping_timer.cancel();
	}

	@Override
	public void onMessage(final String m){
		SvrMonitorMod.LOGGER.info("wsmsg: " + m);
		try{
			final Map<String, Object> msg = JsonUtil.parseJsonToMap(m);
			switch((String)(msg.get("status"))){
				case "pong":
					break;
				case "close":{
					this.running = false;
					super.close();
					SvrMonitorMod.LOGGER.warn("Remote server ask to close web socket");
					break;
				}
			}
		}catch(RuntimeException e){}
	}

	@Override
	public void onError(Exception err){
		SvrMonitorMod.LOGGER.error("Web socket error:\n", err);
	}

	public void sendMessage(final String msg){
		if(super.isOpen()){
			super.send(msg);
		}
	}

	public void sendMessage(final Map<String, Object> msg){
		this.sendMessage(JsonUtil.GSON.toJson(msg));
	}

	public void sendServerStatus(final int code){
		if(this.running && super.isOpen()){
			this.sendMessage(JsonUtil.asMap(
				"status", "status",
				"code", Integer.valueOf(code)
			));
		}
	}

	public void init(){
		this.sendMessage(JsonUtil.asMap(
			"status", "info",
			"interval", Integer.valueOf(Config.INSTANCE.getUploadTime()),
			"ticks", Integer.valueOf(0),
			"cpu_num", Integer.valueOf(InfoUploader.CPU_NUM),
			"java_version", InfoUploader.JAVA_VERSION,
			"os", InfoUploader.OS_NAME + " (" + InfoUploader.OS_ARCH + ") version " + InfoUploader.OS_VERSION,
			"max_mem", Long.valueOf(InfoUploader.MAX_MEMORY),
			"total_mem", Long.valueOf(InfoUploader.getTotalMemory()),
			"used_mem", Long.valueOf(InfoUploader.getUsedMemory()),
			"cpu_load", Double.valueOf(InfoUploader.getCpuLoad()),
			"cpu_time", Double.valueOf(InfoUploader.getCpuTime())
		));
		this.running = true;
	}

	@Override
	public void tick(final int ticks){
		if(this.isbroken){
			this.isbroken = false;
			try{
				if(!super.connectBlocking(5L, TimeUnit.SECONDS)){
					SvrMonitorMod.LOGGER.error("Can not reconnect to remote server.");
				}
			}catch(InterruptedException e){}
		}
		if(this.running && super.isOpen()){
			this.sendMessage(JsonUtil.asMap(
				"status", "info",
				"ticks", Integer.valueOf(ticks),
				"total_mem", Long.valueOf(InfoUploader.getTotalMemory()),
				"used_mem", Long.valueOf(InfoUploader.getUsedMemory()),
				"cpu_load", Double.valueOf(InfoUploader.getCpuLoad()),
				"cpu_time", Double.valueOf(InfoUploader.getCpuTime())
			));
		}
	}

	@Override
	public void close(){
		try{
			if(this.running && super.isOpen()){
				this.sendMessage(JsonUtil.asMap("status", "close"));
			}
		}finally{
			super.close();
		}
	}

	@Override
	public void close(final Throwable err){
		try{
			if(this.running && super.isOpen()){
				this.sendMessage(
					JsonUtil.asMap("status", "close_with_err", "error", err.getMessage()));
			}
		}finally{
			super.close();
		}
	}
}
