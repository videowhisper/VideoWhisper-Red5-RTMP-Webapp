package com.videowhisper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.*;

import org.red5.server.adapter.ApplicationAdapter;
import org.red5.server.api.IConnection;
import org.red5.server.api.scope.IScope;
import org.red5.server.api.Red5;
import org.red5.server.api.service.IServiceCapableConnection;
import org.red5.server.api.stream.IBroadcastStream;
import org.red5.server.stream.ClientBroadcastStream;
import org.red5.server.api.service.IPendingServiceCall;
import org.red5.server.api.service.IPendingServiceCallback;
import org.red5.server.api.stream.IStreamCapableConnection;


public class Application extends ApplicationAdapter {
	public String version = "2.12";
	public String features = "Unlimited rooms/user list,  Room queing (for waiting rooms), Block/Unblock, Private Instant Message,  Room counters, Microphone/Webcam status, User Profile,  Ask/Answer Rights (ie Video Chat), Rename User/Name availability check, Room Profile, Administrators, Archive all Streams, Record Streams on Request, Discard Last Record, As You Type Preview, Bandwidth Detection, Clear Answer"; 
	public Boolean multisessions = false; // allow user in multiple rooms at the
	// same time
	private IScope appScope;
	private String timerId;
	private Vector<vwClient> userlist;

	private Vector<vwRoom> roomlist;
	private Vector<String> usednames;

	public boolean withLogging = false;
	public String logFilename = "videowhisper";

	public String[] allowedDomains = {};

	public String abspath;

	// private String[] allowedDomains = { "localhost", "file:" };
	// allowedDomains={"videowhisper.com","videochat-software.com","videochat-scripts.com"};

	public boolean recordEverything = false;
	public boolean acceptPlayers = true;

	public void setAllowedDomains(String[] domains) {
		allowedDomains = domains;
	}

	public void setWithLogging(boolean logging) {
		withLogging = logging;
	}

	public void setLogFilename(String logfilename) {
		logFilename = logfilename;
	}

	public void setRecordEverything(boolean record) {
		recordEverything = record;
	}

	public void setAcceptPlayers(boolean players) {
		acceptPlayers = players;
	}

	public boolean appStart(IScope app) {

		if (super.appStart(app) == false)
			return false;

		abspath = System.getProperty("red5.webapp.root") + "/" + getName();

		appScope = app;
		userlist = new Vector<vwClient>();

		roomlist = new Vector<vwRoom>();

		usednames = new Vector<String>();

		logReset("Started VideoWhisper v" + version);
		logAppend("\nSupported Features: " + features);
		logAppend("\nAllowed Domains:");
		int i;
		if (allowedDomains.length > 0)
			for (i = 0; i < allowedDomains.length; i++) {
				allowedDomains[i] = new String(allowedDomains[i].trim());
				logAppend(" " + allowedDomains[i]);
			}
		logAppend("\nLog:");

		return true;
	}

	public void appStop() {
		logAppend("\r\rStopped.");
	}

	public class vwClient {
		public String Session, Username, Type, StreamName, ServerName,
				LastRecord;
		public Boolean AutoRecording;

		public long StartTime, LastPing;
		public IConnection Connection;

		public Vector<vwRoom> Rooms;
		public Vector<vwClient> Blocked;
		public HashMap<String, HashMap<String, Boolean>> Answers;
		public HashMap<String, Boolean> DefaultAnswers;
		public HashMap<String, String> DefaultMessages;
		public Object Profile;

		public int CamStatus, MicStatus;

		public vwClient(String Session1, IConnection connection1) {
			Session = Session1;
			Connection = connection1;
			Date now = new Date();
			LastPing = now.getTime();
			StartTime = LastPing;
			StreamName = Session1;
			ServerName = "same";
			LastRecord = "";
			AutoRecording = false;
			Rooms = new Vector<vwRoom>();
			Blocked = new Vector<vwClient>();
			Answers = new HashMap<String, HashMap<String, Boolean>>();
			DefaultAnswers = new HashMap<String, Boolean>();
			DefaultMessages = new HashMap<String, String>();
		}

	}

	public class vwUser {
		public String Session, Username, Type;

		public vwUser(String Session1, String Username1) {
			Session = Session1;
			Username = Username1;
			Type="0";
		}
		
		public vwUser(String Session1, String Username1, String Type1) {
			Session = Session1;
			Username = Username1;
			Type = Type1;
		}
	}

	public class vwRoom {
		public String Name;
		public int Size;
		public Object Profile;

		public IConnection Connection;

		public Vector<vwClient> Queue;
		public Vector<vwClient> Listeners;

		public vwRoom(String Name1) {
			Name = Name1;
			Queue = new Vector<vwClient>();
			Listeners = new Vector<vwClient>();
			Profile = new Object();
			Size = 0;
		}
	}

	public void logReset(String log_text) {

		if (!withLogging)
			return;

		Date now = new Date();
		logFilename = abspath + "/" + logFilename + "-" + now.getTime()
				+ ".txt";

		try {
			// Create file
			FileWriter fstream = new FileWriter(logFilename, false);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(log_text);
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	public void logAppend(String log_text) {
		if (!withLogging)
			return;

		if (log_text == null)
			log_text = new String("=NULL=");

		try {
			// Create file
			FileWriter fstream = new FileWriter(logFilename, true);
			BufferedWriter out = new BufferedWriter(fstream);
			out.write(log_text);
			// Close the output stream
			out.close();
		} catch (Exception e) {// Catch exception if any
			System.err.println("Error: " + e.getMessage());
		}
	}

	private vwRoom n2r(String name) {
		if (roomlist != null)
			if (!roomlist.isEmpty()) {
				vwRoom r;
				Enumeration<vwRoom> e = roomlist.elements();
				while (e.hasMoreElements()) {
					r = (vwRoom) e.nextElement();
					if (r.Name.equals(name))
						return r;
				}
			}
		;
		return null;

	}

	private vwClient s2c(String session) {

		if (userlist != null)
			if (!userlist.isEmpty()) {
				vwClient c;
				Enumeration<vwClient> e = userlist.elements();
				while (e.hasMoreElements()) {
					c = (vwClient) e.nextElement();
					if (c.Session.equals(session))
						return c;
				}
			}
		;

		return null;

	}

	private vwClient c2c(IConnection con) {
		if (userlist != null)
			if (!userlist.isEmpty()) {
				vwClient c;
				Enumeration<vwClient> e = userlist.elements();
				while (e.hasMoreElements()) {
					c = (vwClient) e.nextElement();
					if (c.Connection.equals(con))
						return c;
				}
			}
		;
		return null;
	}

	private vwClient cc() {
		IConnection con = Red5.getConnectionLocal();
		return c2c(con);
	}

	private void cast(String clientFunction, Object[] params, vwClient client) {
		// logAppend("\rCasting:"+clientFunction+" @Session: "+client.Session);

		if (client.Connection instanceof IServiceCapableConnection) {
			IServiceCapableConnection sc = (IServiceCapableConnection) client.Connection;
			sc.invoke(clientFunction, params);
		}
	}

	private void broadcast(String clientFunction, Object[] params, vwRoom room) {
		// logAppend("\rBroadcasting:"+clientFunction+" "+params[0]+" @Room: "+room.Name+" : ");

		vwClient c;
		Enumeration<vwClient> e = room.Queue.elements();
		while (e.hasMoreElements()) {
			c = (vwClient) e.nextElement();
			if (c.Connection instanceof IServiceCapableConnection) {
				IServiceCapableConnection sc = (IServiceCapableConnection) c.Connection;
				sc.invoke(clientFunction, params);
				// logAppend(c.Session+", ");
			}
		}
	}

	private void broadcastListeners(String clientFunction, Object[] params,
			vwRoom room) {
		// logAppend("\rBroadcasting Listeners:"+clientFunction+" "+params[0]+" @Room: "+room.Name+" : ");

		vwClient c;
		Enumeration<vwClient> e = room.Listeners.elements();
		while (e.hasMoreElements()) {
			c = (vwClient) e.nextElement();
			if (c.Connection instanceof IServiceCapableConnection) {
				IServiceCapableConnection sc = (IServiceCapableConnection) c.Connection;
				sc.invoke(clientFunction, params);
				// logAppend(c.Session+", ");
			}
		}
	}

	private void broadcastNeighbours(String clientFunction, Object[] params,
			vwClient client) {
		// logAppend("\rBroadcasting Neighbours:"+clientFunction+" @Session: "+client.Session);
		vwRoom r;
		Enumeration<vwRoom> e = client.Rooms.elements();
		while (e.hasMoreElements()) {
			r = e.nextElement();
			int i;
			for (i = 0; i < params.length; i++)
				if (params[i] != null)
					if (params[i].equals("vw_room"))
						params[i] = r.Name;
			broadcast(clientFunction, params, r);
		}
	}

	private boolean urlDenied(String url) {

		if (allowedDomains.length == 0)
			return false;

		String domain;
		domain = url.replaceAll("http://", "");
		domain = domain.replaceAll("http://", "");
		domain = domain.replaceAll("http://", "");
		domain = domain.replaceAll("www.", "");

		if (domain.indexOf('/') >= 0)
			domain = domain.substring(0, domain.indexOf('/'));
		logAppend(" Domain:" + domain);

		for (short i = 0; i < allowedDomains.length; i++)
			if (domain.equals(allowedDomains[i]))
				return false;
		return true;
	}

	public boolean appConnect(IConnection con, Object[] params) {
		logAppend("\nConnecting: ");

		String Session1;
		boolean Player = false;

		if (params.length > 0)
			Session1 = (String) params[0];
		else {
			Player = true;
			Session1 = "Player" + String.valueOf(System.currentTimeMillis());
		}

		logAppend(Session1);

		String swfUrl = (String) con.getConnectParams().get("swfUrl");
		logAppend(" From: " + swfUrl);

		if (urlDenied(swfUrl)) {
			rejectClient("Connection not allowed!");
			logAppend(" was REJECTED (url denied)");
		}

		if (!acceptPlayers && Player) {
			rejectClient("Players are not allowed!");
			logAppend(" was REJECTED (players disabled)");
		} else if (s2c(Session1) != null) {
			rejectClient(Session1 + " already connected!");
			logAppend(" was REJECTED (duplicate)");
		} else {
			logAppend(" was CONNECTED");
			vwClient c = new vwClient(Session1, con);
			userlist.add(c);
		}

		return true;
	}

	public void adminUpdateServer() {

		vwClient client = cc();

		HashMap<String, HashMap<String, Object>> publicusers = new HashMap<String, HashMap<String, Object>>();

		if (userlist != null)
			if (!userlist.isEmpty()) {
				vwClient c;
				Enumeration<vwClient> e = userlist.elements();
				while (e.hasMoreElements()) {
					c = (vwClient) e.nextElement();
					HashMap<String, Object> publicuser = new HashMap<String, Object>();
					publicuser.put("Session", c.Session);
					publicuser.put("Username", c.Username);
					publicuser.put("CamStatus", c.CamStatus);
					publicuser.put("MicStatus", c.MicStatus);
					publicuser.put("Identify", c.Connection
							.getRemoteAddresses());

					String publicroomslist = "";
					if (!c.Rooms.isEmpty()) {
						vwRoom r;
						Enumeration<vwRoom> e2 = c.Rooms.elements();
						while (e2.hasMoreElements()) {
							r = e2.nextElement();
							if (publicroomslist.length() > 0)
								publicroomslist = publicroomslist + ", ";
							publicroomslist = publicroomslist + r.Name;
						}
					}
					publicuser.put("RoomsList", publicroomslist);

					publicusers.put(c.Session, publicuser);
				}
			}

		cast("adminUpdateFromServer", new Object[] { publicusers, null, null },
				client);
		// HashMap<String, Object>
	}

	public void adminKickServer(String Session1) {
		vwClient client = s2c(Session1);
		// appDisconnect(client.Connection);
		client.Connection.close();
	}

	public void myDetailsServer(String Username1, String Type1) {

		vwClient client = cc();

		if (client != null) {
			client.Username = Username1;
			client.Type = Type1;
			usednames.add(Username1);
		}

	}

	public void bandwidthServer() {
		IConnection con = Red5.getConnectionLocal();
		
        BandwidthDetection detect = new BandwidthDetection();
        detect.checkBandwidth(con);
        
		//measureBandwidth(con);
	}

	public void pingServer() {
		vwClient client = cc();
		Date now = new Date();
		long timeStamp = now.getTime();
		cast("pingFromServer", new Object[] { timeStamp }, client);
	}

	public void pongServer(long timeStamp) {
		vwClient client = cc();

		if (client != null) {
			Date now = new Date();
			client.LastPing = now.getTime();
		}

	}

	public void defaultAnswerUserServer(Boolean Answer, String Type,
			String Message) {
		vwClient client = cc();
		if (Answer == null) {
			logAppend("\n" + client.Username + " clearing default answer for "
					+ Type);
			if (client.DefaultAnswers.containsKey(Type))
				client.DefaultAnswers.remove(Type);			
			if (client.DefaultMessages.containsKey(Type))
				client.DefaultMessages.remove(Type);
		} else {
			client.DefaultAnswers.put(Type, Answer);
			client.DefaultMessages.put(Type, Message);
			logAppend("\n" + client.Username + " setting default answer for "
					+ Type + " to " + Answer);
		}
	}
	
	public void clearAnswerUserServer(String Type) {
		vwClient client = cc();
			logAppend("\n" + client.Username + " clearing default answer for "
					+ Type);

			if (client.DefaultAnswers.containsKey(Type))
				client.DefaultAnswers.remove(Type);			
			if (client.DefaultMessages.containsKey(Type))
				client.DefaultMessages.remove(Type);
	}

	public void askUserServer(String Type, String Message, String User) {
		vwClient client = cc();
		if (client == null) return;
		vwClient c = s2c(User);
		if (c != null)
			if (!c.Blocked.contains(client))
				if (c.DefaultAnswers.containsKey(Type))
					cast("answerFromServer", new Object[] {
							c.DefaultAnswers.get(Type), Type,
							c.DefaultMessages.get(Type), c.Session }, client);
				else
					cast("askFromServer", new Object[] { Type, Message,
							client.Session }, c);
			else
				cast("answerFromServer", new Object[] { false, Type,
						"Sorry, user " + c.Username + " has blocked you.",
						c.Session }, client);
	}

	public void answerUserServer(Boolean Answer, String Type, String Message,
			String User) {
		vwClient client = cc();
		vwClient c = s2c(User);
		if (c != null)
			if (!c.Blocked.contains(client)) {

				HashMap<String, Boolean> a;

				if (!client.Answers.containsKey(c.Session)) {
					a = new HashMap<String, Boolean>();
					a.put(Type, Answer);
					client.Answers.put(c.Session, a);
				} else {
					a = client.Answers.get(c.Session);
					a.put(Type, Answer);
				}

				cast("answerFromServer", new Object[] { Answer, Type, Message,
						client.Session }, c);

			} else
				cast("answerFromServer", new Object[] { false, Type,
						"Sorry, user " + c.Username + " has blocked you.",
						c.Session }, client);

	}

	public void streamPublishStart(IBroadcastStream stream) {
		vwClient client = cc();
		String stream1 = stream.getPublishedName();
		client.StreamName = stream1;

		if (recordEverything) {
			try {
				String Record1 = stream1 + "-"
						+ String.valueOf(System.currentTimeMillis());
				stream.saveAs(Record1, false);
				client.AutoRecording = true;

				client.LastRecord = stream.getSaveFilename();
			} catch (Exception e) {
				logAppend("Error while automatically saving stream: " + stream1);
			}
		}

	}

	public void recordStreamServer(String Stream1) {

		IConnection con = Red5.getConnectionLocal();
		ClientBroadcastStream stream = (ClientBroadcastStream) getBroadcastStream(
				con.getScope(), Stream1);
		String Record1 = Stream1 + "-"
				+ String.valueOf(System.currentTimeMillis());

		try {
			// Save the stream to disk.
			stream.saveAs(Record1, true);
			logAppend("\rRecording: " + stream.getSaveFilename());
		} catch (Exception e) {
			logAppend("\rError while on request saving stream: " + Stream1);
		}

		vwClient client = cc();
		client.LastRecord = stream.getSaveFilename();
		cast("recordingStreamFromServer", new Object[] { Stream1, Record1 },
				client);
	}

	public void deleteLastRecord() {

		vwClient client = cc();
		if (client.LastRecord.length() > 0) {
			logAppend("\rDiscarding record: " + client.LastRecord);
			try {
				File record = new File(client.LastRecord);
				if (!record.exists())
					record = new File(abspath + "/" + client.LastRecord);

				if (!record.exists())
					logAppend(" Not Found " + record.getAbsolutePath());
				else {
					if (!record.delete()) {
						logAppend(" Failed to delete "
								+ record.getAbsolutePath());
						record.deleteOnExit();
					}

					String metafilename = record.getAbsolutePath() + ".meta";
					File metafile = new File(metafilename);

					if (!metafile.exists())
						logAppend(" Not Found " + metafilename);
					else if (!metafile.delete()) {
						logAppend(" Failed to delete " + metafilename);
						metafile.deleteOnExit();
					}
				}
			} catch (Exception e) {
				logAppend(" Error while trying to delete");
			}
		}
	}

	public void registerStreamServer(String Stream1, String Server1) {
		vwClient client = cc();
		client.StreamName = Stream1;
		client.ServerName = Server1;
	}

	public void requestStreamServer(String Session1) {
		vwClient client = cc();
		vwClient target = s2c(Session1);
		cast("streamFromServer", new Object[] { target.StreamName,
				target.ServerName, Session1 }, client);
	}

	public void myProfileServer(Object Profile1) {
		logAppend("\r Profile Server: ");

		vwClient client = cc();

		if (client != null) {
			client.Profile = Profile1;
			broadcastNeighbours("userProfileFromServer", new Object[] {
					client.Profile, client.Session, "vw_room" }, client);
			logAppend(client.Profile.toString() + " @" + client.Session);
		}
	}

	public void roomProfileServer(Object Profile1, String Room1) {
		logAppend("\r Room Profile Server: " + Profile1.toString() + " @"
				+ Room1);

		vwRoom room = n2r(Room1);
		room.Profile = Profile1;

		broadcast("roomProfileFromServer",
				new Object[] { room.Profile, Room1 }, room);

	}

	public void myOutputServer(int camStatus1, int micStatus1) {
		vwClient client = cc();

		if (client != null) {
			client.CamStatus = camStatus1;
			client.MicStatus = micStatus1;
			broadcastNeighbours("userOutputFromServer", new Object[] {
					client.CamStatus, client.MicStatus, client.Session,
					"vw_room" }, client);

		}
	}

	public void myRoomsListServer(HashMap<String, Object> Roomlist) {
		vwClient client = cc();

		Iterator iterator = Roomlist.keySet().iterator();
		while (iterator.hasNext()) {
			String rn = iterator.next().toString();
			vwRoom r = n2r(rn);
			if (r != null) {
				Roomlist.put(rn, r.Size);
				if (!r.Listeners.contains(client))
					r.Listeners.add(client);
			}
		}

		cast("listedRoomsFromServer", new Object[] { Roomlist }, client);

		// logAppend("\rRoomlist:"+Roomlist.toString()+" "+Roomlist.getClass());
	}

	public void leaveRoomServer(String Room1) {
		vwClient client = cc();
		vwRoom r = n2r(Room1);

		if (r.Queue.contains(client)) {
			broadcast("userLeftFromServer", new Object[] {
					new vwUser(client.Session, client.Username, client.Type), r.Name }, r);

			int pos = r.Queue.indexOf(client);

			r.Queue.remove(client);
			r.Size--;
			if (r.Size > 0) {
				// notify new positions
				int i;
				for (i = pos; i < r.Queue.size(); i++) {
					vwClient c = r.Queue.elementAt(i);
					int cpos = r.Queue.indexOf(c) + 1;
					cast("queueFromServer", new Object[] { cpos, r.Name }, c);
				}
			}

			// notify new size
			broadcastListeners("listedRoomFromServer", new Object[] { r.Name,
					r.Size }, r);
		}

	}

	public void joinRoomServer(String Room1) {

		vwClient client = cc();
		if (client == null)	return;

		vwRoom room = n2r(Room1);
	
		logAppend("\r" + client.Session + " joins room: " + Room1);

		if (!multisessions) {
			// remove user from all other rooms
			vwRoom r;
			Enumeration<vwRoom> e = roomlist.elements();
			while (e.hasMoreElements()) {

				r = e.nextElement();
				if (r.Queue.contains(client)) {
					broadcast(
							"userLeftFromServer",
							new Object[] {
									new vwUser(client.Session, client.Username, client.Type),
									r.Name }, r);
					int pos = r.Queue.indexOf(client);

					r.Queue.remove(client);
					r.Size--;
					if (r.Size > 0) {
						// notify new positions
						int i;
						for (i = pos; i < r.Queue.size(); i++) {
							vwClient c = r.Queue.elementAt(i);
							int cpos = r.Queue.indexOf(c) + 1;
							cast("queueFromServer",
									new Object[] { cpos, r.Name }, c);
						}
					}
					// notify new size
					broadcastListeners("listedRoomFromServer", new Object[] {
							r.Name, r.Size }, r);
				}

			}
		}

		if (room == null) {
			room = new vwRoom(Room1);
			room.Queue.add(client);
			room.Listeners.add(client);
			room.Size = 1;
			roomlist.add(room);
		} else {
			room.Queue.add(client);
			room.Listeners.add(client);
			room.Size++;
		}

		// client gets info from room
		int pos = room.Queue.indexOf(client) + 1;
		cast("queueFromServer", new Object[] { pos, room.Name }, client);
		cast("roomProfileFromServer", new Object[] { room.Profile, room.Name },
				client);

		// notify everybody (including self)
		broadcast("userFromServer", new Object[] {
				new vwUser(client.Session, client.Username, client.Type), room.Name }, room);
		broadcast("userOutputFromServer", new Object[] { client.CamStatus,
				client.MicStatus, client.Session, room.Name }, room);
		broadcast("userProfileFromServer", new Object[] { client.Profile,
				client.Session, room.Name }, room);

		if (!client.Rooms.contains(room))
			client.Rooms.add(room);

		// notify room owners about new size
		broadcastListeners("listedRoomFromServer", new Object[] { room.Name,
				room.Size }, room);

		// add everybody's info to client
		vwClient c;
		Enumeration<vwClient> e = room.Queue.elements();
		while (e.hasMoreElements()) {
			c = (vwClient) e.nextElement();
			if (!c.equals(client)) {
				cast("userFromServer", new Object[] {
						new vwUser(c.Session, c.Username, c.Type), room.Name }, client);
				cast("userOutputFromServer", new Object[] { c.CamStatus,
						c.MicStatus, c.Session, room.Name }, client);
				cast("userProfileFromServer", new Object[] { c.Profile,
						c.Session, room.Name }, client);
			}

		}

	}

	@Override
	public void appDisconnect(IConnection conn) {
		logAppend("\rDisconnecting: ");
		vwClient client = c2c(conn);
		if (client != null) {
			logAppend(client.Session);

			// remove user from all rooms and room listeners
			vwRoom r;
			Enumeration<vwRoom> e = roomlist.elements();
			while (e.hasMoreElements()) {
				r = e.nextElement();
				if (r.Listeners.contains(client))
					r.Listeners.remove(client);
				if (r.Queue.contains(client)) {
					int pos = r.Queue.indexOf(client);

					r.Queue.remove(client);
					r.Size--;

					// notify new positions
					int i;
					for (i = pos; i < r.Queue.size(); i++) {
						vwClient c = r.Queue.elementAt(i);
						int cpos = r.Queue.indexOf(c) + 1;
						cast("queueFromServer", new Object[] { cpos, r.Name },
								c);
					}
					// notify new size
					broadcast(
							"userLeftFromServer",
							new Object[] {
									new vwUser(client.Session, client.Username, client.Type),
									r.Name }, r);
					broadcastListeners("listedRoomFromServer", new Object[] {
							r.Name, r.Size }, r);

				}

			}
			
			// remove from list
			userlist.remove(client);
			// release used name
			if (usednames.contains(client.Username)) usednames.remove(client.Username);
		}
		super.appDisconnect(conn);
	}

	public void messageServer(String Message1, String Room1) {
		vwClient client = cc();
		vwRoom room = n2r(Room1);

		if (room != null) {
			vwClient c;
			Enumeration<vwClient> e = room.Queue.elements();
			while (e.hasMoreElements()) {
				c = (vwClient) e.nextElement();
				if ((!c.Blocked.contains(client)) && (!c.equals(client)))
					if (c.Connection instanceof IServiceCapableConnection) {
						IServiceCapableConnection sc = (IServiceCapableConnection) c.Connection;
						sc.invoke("messageFromServer", new Object[] { Message1,
								Room1 });
						logAppend("\rMessage: " + client.Session + ": "
								+ Message1 + " " + c.Session + "@" + Room1);
					}
			}
		}
	}

	public void privateMessageServer(String Message1, String Session1) {
		vwClient client = cc();
		vwClient c = s2c(Session1);

		if (c != null)
			if (!c.Blocked.contains(client))
				cast("privateMessageFromServer", new Object[] { Message1,
						client.Session }, c);
			else
				cast("privateMessageFromServer", new Object[] {
						Session1 + " has blocked your messages.", c.Session },
						client);
		else
			cast("privateMessageFromServer", new Object[] {
					Session1 + " is offline.", Session1 }, client);
	}

	public void asYouTypeServer(String Session1, Boolean Par1, String Par2, String Par3, String Par4 ) {
		vwClient client = cc();
		vwClient c = s2c(Session1);

		if (c != null)
			if (!c.Blocked.contains(client))
				cast("asYouTypeFromServer", new Object[] {client.Session, Par1, Par2, Par3, Par4 }, c);

	}

	
	public void commandServer(String Command1, String Room1) {
		vwRoom room = n2r(Room1);
		vwClient c;
		Enumeration<vwClient> e = userlist.elements();
		while (e.hasMoreElements()) {
			c = (vwClient) e.nextElement();
			if (Room1.equals("__All__") || c.Rooms.contains(room))
				if (c.Connection instanceof IServiceCapableConnection) {
					IServiceCapableConnection sc = (IServiceCapableConnection) c.Connection;
					sc.invoke("commandFromServer", new Object[] { Command1,
							Room1 });
					// logAppend(c.Session+", ");
				}
		}

	}

	public void changeUsernameServer(String newName) {
		vwClient client = cc();
		logAppend("Changing username to '" + newName + "' for "
				+ client.Session);
		String error_message = "";

		if (usednames.contains(newName)) {
			error_message = "That name (" + newName + ") is already taken.";
			cast("userRenameFromServer", new Object[] { newName,
					client.Session, "", error_message }, client);
		} else {
			usednames.remove(client.Username);
			usednames.add(newName);
			client.Username = newName;
			broadcastNeighbours("userRenameFromServer", new Object[] { newName,
					client.Session, "vw_room", error_message }, client);

		}
	}

	public void blockUserServer(String Session1) {
		vwClient client = cc();
		vwClient c = s2c(Session1);
		if (c != null)
			if (!client.Blocked.contains(c))
				client.Blocked.add(c);
	}

	public void unblockUserServer(String Session1) {
		vwClient client = cc();
		vwClient c = s2c(Session1);
		if (c != null)
			if (client.Blocked.contains(c))
				client.Blocked.remove(c);
	}
	
	public interface IBandwidthDetection {
	       
		   public void checkBandwidth(IConnection p_client);
		       
		   public void calculateClientBw(IConnection p_client);
		}

	
	public class BandwidthDetection implements IPendingServiceCallback, IBandwidthDetection {
		   
		   IConnection client = null;
		   double latency = 0;
		   double cumLatency = 1;
		   int count = 0;
		   int sent = 0;
		   double kbitDown = 0;
		   double deltaDown = 0;
		   double deltaTime = 0;
		   
		   List<Long> pakSent = new ArrayList<Long>();
		   List<Long> pakRecv = new ArrayList<Long>();
		   
		   private Map<String, Long> beginningValues;
		   private double[] payload = new double[1200];
		   private double[] payload_1 = new double[12000];
		   private double[] payload_2 = new double[12000];
		   
		   //private static final Logger log = Logger.getLogger(BandwidthDetection.class.getName());
		   
		   public BandwidthDetection()
		   {
		       
		   }
		   
		   public void checkBandwidth(IConnection p_client)
		   {
		       this.calculateClientBw(p_client);
		   }
		   
		   public void calculateClientBw(IConnection p_client)
		   {
		       for (int i=0; i<1200; i++){
		           payload[i] = Math.random();
		       }
		       
		       p_client.setAttribute("payload", payload);
		       
		       for (int i=0; i<12000; i++){
		           payload_1[i] = Math.random();
		       }
		       
		       p_client.setAttribute("payload_1", payload_1);
		       
		       for (int i=0; i<12000; i++){
		           payload_2[i] = Math.random();
		       }
		       
		       p_client.setAttribute("payload_2", payload_2);
		       
		       final IStreamCapableConnection beginningStats = this.getStats();
		       final Long start = new Long(System.nanoTime()/1000000); //new Long(System.currentTimeMillis());
		       
		       this.client = p_client;
		       beginningValues = new HashMap<String, Long>();
		       beginningValues.put("b_down", beginningStats.getWrittenBytes());
		       beginningValues.put("b_up", beginningStats.getReadBytes());
		       beginningValues.put("time", start);
		       
		       this.pakSent.add(start);
		       this.sent++;
		       this.callBWCheck("");
		   }
		   
		   /**
		    * Handle callback from service call. 
		    */
		   public void resultReceived(IPendingServiceCall call) { 
		       Long now = new Long(System.nanoTime()/1000000); //new Long(System.currentTimeMillis());
		       this.pakRecv.add(now);
		       Long timePassed = (now - this.beginningValues.get("time"));
		       this.count++;
		       
		       if (count == 1) {
		           latency = Math.min(timePassed, 800);
		           latency = Math.max(latency, 10);
		           
		           //log.info("count: "+count+ " sent: "+sent+" timePassed: "+timePassed+" latency: "+latency);
		           
		           // We now have a latency figure so can start sending test data.
		           // Second call.  1st packet sent
		           pakSent.add(now);
		           sent++;
		           
		           this.callBWCheck(this.client.getAttribute("payload"));
		       }
		       // To run a very quick test, uncomment the following if statement and comment out the next 3 if statements.
		       /*
		       else if (count == 2 && (timePassed < 2000)) {
		           pakSent.add(now1);
		           sent++;
		           cumLatency++;
		           this.callBWCheck(this.client.getAttribute("payload"));
		       }
		       */
		       // The following will progressivly increase the size of the packets been sent until 1 second has elapsed.
		       else if ((count > 1 && count < 3) && (timePassed < 1000)) {
		           pakSent.add(now);
		           sent++;
		           cumLatency++;
		           this.callBWCheck(this.client.getAttribute("payload"));
		       } else if ((count >=3 && count < 6) && (timePassed < 1000)) {
		           pakSent.add(now);
		           sent++;
		           cumLatency++;
		           this.callBWCheck(this.client.getAttribute("payload_1"));
		       } else if (count >= 6 && (timePassed < 1000)) {
		           pakSent.add(now);
		           sent++;
		           cumLatency++;
		           this.callBWCheck(this.client.getAttribute("payload_2"));
		       }
		       // Time elapsed now do the calcs
		       else if (sent == count) {
		           // see if we need to normalize latency
		           if (latency >= 100) {
		               // make sure satelite and modem is detected properly
		               if (pakRecv.get(1) - pakRecv.get(0) > 1000) {
		                   latency = 100; 
		               }
		           }
		   
		           this.client.removeAttribute("payload");
		           this.client.removeAttribute("payload_1");
		           this.client.removeAttribute("payload_2");
		           
		           final IStreamCapableConnection endStats = this.getStats();           
		           deltaDown = (endStats.getWrittenBytes() - beginningValues.get("b_down")) * 8 / 1000; // bytes to kbits
		           deltaTime = ((now - beginningValues.get("time")) - (latency * cumLatency)) / 1000; // total dl time - latency for each packet sent in secs
		           if (deltaTime <= 0) {
		               deltaTime = (now - beginningValues.get("time")) / 1000;
		           }
		           kbitDown = Math.round(deltaDown / deltaTime); // kbits / sec
		           
		          // log.info("onBWDone: kbitDown = " + kbitDown + ", deltaDown= " + deltaDown + ", deltaTime = " + deltaTime + ", latency = " + this.latency);
		           
		           this.callBWDone(this.kbitDown, this.deltaDown, this.deltaTime, this.latency);                                 
		       }
		   }
		   
		   private void callBWCheck(Object params)
		   {
		       IConnection conn = Red5.getConnectionLocal();
		       
		       if (conn instanceof IServiceCapableConnection) {
		           ((IServiceCapableConnection) conn).invoke("onBWCheck", new Object[]{params}, this);
		       }
		   }
		   
		   private void callBWDone(double kbitDown, double deltaDown, double deltaTime, double latency)
		   {
		       IConnection conn = Red5.getConnectionLocal();
		               
		       if (conn instanceof IServiceCapableConnection) {
		           ((IServiceCapableConnection) conn).invoke("onBWDone", new Object[]{kbitDown,  deltaDown, deltaTime, latency});
		       }
		   }
		   
		   private IStreamCapableConnection getStats()
		   {
		       IConnection conn = Red5.getConnectionLocal();
		       if (conn instanceof IStreamCapableConnection) {
		           return (IStreamCapableConnection) conn;
		       }
		       return null;
		   }
		}

}