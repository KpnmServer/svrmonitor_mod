
package com.github.kpnmserver.svrmonitor_mod.connect;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.net.URI;
import java.net.URL;
import java.net.MalformedURLException;
import java.net.HttpURLConnection;
import java.net.ConnectException;
import java.util.UUID;
import java.util.Map;

import com.github.kpnmserver.svrmonitor_mod.SvrMonitorMod;
import com.github.kpnmserver.svrmonitor_mod.connect.InfoUploader;
import com.github.kpnmserver.svrmonitor_mod.storage.Config;
import com.github.kpnmserver.svrmonitor_mod.util.JsonUtil;

public final class HttpInfoUploader implements InfoUploader {
	private final String id;
	private final URL url;
	private String svrid = "";
	private boolean alive = false;

	public HttpInfoUploader(final String url) throws MalformedURLException{
		this(new URL(url));
	}

	public HttpInfoUploader(final URI uri) throws MalformedURLException{
		this(uri.toURL());
	}

	public HttpInfoUploader(final URL url){
		this.url = url;
		this.id = UUID.randomUUID().toString();
		this.init();
	}

	public void init(){
		final String msg = this.sendMessage(JsonUtil.asMap(
			"id", this.id,
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
		this.sendServerStatus(SvrMonitorMod.INSTANCE.getStatusCode());
		if(msg != null){
			Map<String, Object> msgobj = JsonUtil.parseJsonToMap(msg);
			this.svrid = (String)(msgobj.get("id"));
		}
	}

	@Override
	public void sendServerStatus(final int code){
		this.sendMessage(JsonUtil.asMap(
			"id", this.id,
			"status", "status",
			"code", Integer.valueOf(code)
		));
	}

	@Override
	public boolean isalive(){
		return this.alive;
	}

	@Override
	public void tick(final int ticks){
		final String msg = this.sendMessage(JsonUtil.asMap(
			"id", this.id,
			"status", "info",
			"ticks", Integer.valueOf(ticks),
			"total_mem", Long.valueOf(InfoUploader.getTotalMemory()),
			"used_mem", Long.valueOf(InfoUploader.getUsedMemory()),
			"cpu_load", Double.valueOf(InfoUploader.getCpuLoad()),
			"cpu_time", Double.valueOf(InfoUploader.getCpuTime())
		));
		if(msg != null){
			Map<String, Object> msgobj = JsonUtil.parseJsonToMap(msg);
			if(!this.svrid.equals(msgobj.get("id"))){
				this.init();
			}
		}
	}

	@Override
	public void close(){
		this.sendMessage(JsonUtil.asMap("id", this.id, "status", "close"));
	}

	@Override
	public void close(final Throwable err){
		this.sendMessage(
			JsonUtil.asMap("id", this.id, "status", "close_with_err", "error", err.getMessage()));
	}

	private String sendMessage(final Map<String, Object> msg){
		return this.sendMessage(JsonUtil.GSON.toJson(msg));
	}

	private String sendMessage(final String body){
		HttpURLConnection connection = null;
		OutputStream outstream = null;
		InputStream instream = null;
		Exception err = null;
		for(int i = 0; i < Config.INSTANCE.getTryMaxNum() ;i++){
			try{
				connection = (HttpURLConnection)(this.url.openConnection());
				connection.setRequestMethod("POST");
				connection.setConnectTimeout(1000);
				connection.setReadTimeout(1500);
				connection.setDoOutput(true);
				connection.setRequestProperty("Content-Type", "application/json");
				outstream = connection.getOutputStream();
				outstream.write(body.getBytes());
				final int code = connection.getResponseCode();
				if(code != 200){
					if(!Config.INSTANCE.isIgnoreCode(code)){
						err = new RuntimeException("Http code: " + code);
					}
				}else{
					instream = connection.getInputStream();
					final BufferedReader reader = new BufferedReader(new InputStreamReader(instream, StandardCharsets.UTF_8));
					String line;
					final StringBuilder sbuf = new StringBuilder();
					while((line = reader.readLine()) != null){
						sbuf.append(line).append('\n');
					}
					this.alive = true;
					return sbuf.toString();
				}
			}catch(IOException e){
				err = e;
			}finally{
				if(outstream != null){try{
					outstream.close();
				}catch(IOException e){
					SvrMonitorMod.LOGGER.error(e);
				}}
				if(instream != null){try{
					instream.close();
				}catch(IOException e){
					SvrMonitorMod.LOGGER.error(e);
				}}
				connection.disconnect();
			}
			this.alive = false;
			try{
				Thread.sleep(500);
			}catch(InterruptedException e){
				return null;
			}
		}
		if(err != null){
			if(err instanceof ConnectException){
				if(Config.INSTANCE.getIgnoreRefused()){
					SvrMonitorMod.LOGGER.error("Connection refused");
				}
			}else{
				SvrMonitorMod.LOGGER.error("Exception in <n> times: " + err.getMessage());
			}
		}
		return null;
	}
}
