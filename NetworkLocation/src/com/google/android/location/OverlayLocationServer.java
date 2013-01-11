package com.google.android.location;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import com.google.android.location.data.LocationData;

import android.util.Log;

public class OverlayLocationServer extends Thread {

	private static final String TAG = "LocationOverlay";

	public OverlayLocationServer(LocationData data,
			NetworkLocationService service) {
		this.data = data;
		this.service = service;
	}

	private double lat = 0;
	private double lon = 0;
	private double alt = 100;
	private LocationData data;
	private boolean enabled = true;
	private NetworkLocationService service;

	public class SocketThread extends Thread {
		Socket socket;

		public SocketThread(Socket socket) {
			this.socket = socket;
		}

		@Override
		public void run() {
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(
						socket.getInputStream()));
				String line = reader.readLine();
				while (enabled && line != null) {
					String[] args = line.split(":");
					if (args.length == 1) {
						args = line.split(";");
					}
					if (args.length == 1) {
						args = line.split(",");
					}
					if (args.length >= 2) {
						lat = Double.parseDouble(args[0]);
						lon = Double.parseDouble(args[1]);
						if (args.length >= 3) {
							alt = Double.parseDouble(args[0]);
						}
						data.setOverlayLocation(lat, lon, alt);
					}
					line = reader.readLine();
				}
				reader.close();
				socket.close();
			} catch (IOException e) {
				Log.w(TAG, e);
				return;
			} finally {
				if (reader != null) {
					try {
						reader.close();
					} catch (IOException e) {
						Log.w(TAG, e);
					}
				}
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						Log.w(TAG, e);
					}
				}
			}

		}
	}

	public void disable() {
		enabled = false;
	}

	@Override
	public void run() {
		ServerSocket server = null;
		try {
			server = new ServerSocket(13371);
			Socket socket = null;
			while ((socket = server.accept()) != null) {
				new SocketThread(socket).start();
			}
		} catch (IOException e) {
			Log.w(TAG, e);
			return;
		} finally {
			if (server != null) {
				try {
					server.close();
				} catch (IOException e) {
					Log.w(TAG, e);
					return;
				}
			}
		}
		service.reInitOverlayServer();
	}
}
