
package com.github.kpnmserver.svrmonitor_mod.connect;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import com.github.kpnmserver.svrmonitor_mod.SvrMonitorMod;
import com.github.kpnmserver.svrmonitor_mod.connect.InfoUploader;
import com.github.kpnmserver.svrmonitor_mod.storage.Config;
import com.github.kpnmserver.svrmonitor_mod.util.JsonUtil;

public final class WSInfoUploader extends WebSocketClient implements InfoUploader{
	private volatile boolean running = false;
	private volatile int sending_count = 0;

	public WSInfoUploader(final String url) throws URISyntaxException {
		this(new URI(url));
	}

	public WSInfoUploader(final URI uri){
		super(uri);
		try{
			if(!super.connectBlocking(5L, TimeUnit.SECONDS)){
				SvrMonitorMod.LOGGER.error("Can not connect to remote server " + uri.toString());
			}
		}catch(InterruptedException e){}
	}

	public boolean isRunning(){
		return this.running;
	}

	@Override
	public void onOpen(ServerHandshake shake){
		this.init();
	}

	@Override
	public void onClose(int paramInt, String paramString, boolean paramBoolean){
		this.running = false;
	}

	@Override
	public void onMessage(final String m){
		try{
			final Map<String, Object> msg = JsonUtil.parseJsonToMap(m);
			switch((String)(msg.get("status"))){
				case "init":{
					final String tk = (String)(msg.get("token"));
					Config.INSTANCE.setUploadToken(tk);
					Config.INSTANCE.save();
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
					break;
				}
				case "token_err":{
					SvrMonitorMod.LOGGER.error("Web socket auth token is wrong");
					break;
				}
				case "change_token":{
					final String tk = (String)(msg.get("token"));
					Config.INSTANCE.setUploadToken(tk);
					Config.INSTANCE.save();
					break;
				}
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

	public void sendServerStatus(final String status){
		if(this.running && super.isOpen()){
			this.sendMessage(JsonUtil.asMap(
				"status", "status",
				"server", status
			));
		}
	}

	public void init(){
		this.sendMessage(JsonUtil.asMap(
			"status", "init",
			"token", Config.INSTANCE.getUploadToken()
		));
	}

	@Override
	public void tick(final int ticks){
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
