package net.njcp.service.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

public class QSFtp extends QFtpUtil {

	static final String name = "SFTP";

	private Session session;
	private ChannelSftp channel;

	@SuppressWarnings("serial")
	public static final Properties DEFAULT_CONFIG = new Properties() {
		{
			put("PreferredAuthentications", "keyboard-interactive,password");
			// put("UserKnownHostsFile", "/dev/null");
			put("StrictHostKeyChecking", "no");
			// put("userauth.gssapi-with-mic", "no");
			// put("ConnectTimeout", "20");
			// put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group-exchange-sha256,diffie-hellman-group-exchange-sha1");
		}
	};

	QSFtp() {
		super();
	}

	public QSFtp(String host, String user, String passwd) {
		super(host, user, passwd);
	}

	public QSFtp(String host, String user, String passwd, int port) {
		super(host, user, passwd, port);
	}

	@Override
	public void connectNLogin() throws Exception {

		this.session = new JSch().getSession(getUser(), getHost(), getPort());
		if ( this.session == null ) {
			throw new QFtpException(I18N.tr("Session is null."));
		}
		this.session.setPassword(getPasswd());
		this.session.setConfig(DEFAULT_CONFIG);
		if ( getConfig() != null ) {
			this.session.setConfig(getConfig());
		}
		this.session.connect();
		this.channel = (ChannelSftp) this.session.openChannel("sftp");
		if ( this.channel == null ) {
			throw new QFtpException(I18N.tr("SFTP Channel is null."));
		}
		this.channel.connect();
	}

	@Override
	public void LogoutNdisconnect() throws Exception {
		if ( this.channel != null ) {
			this.channel.disconnect();
		}
		if ( this.session != null ) {
			this.session.disconnect();
		}
	}

	public Session getSession() {
		return this.session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public ChannelSftp getChannel() {
		return this.channel;
	}

	public void setChannel(ChannelSftp channel) {
		this.channel = channel;
	}

	@Override
	boolean isClosed() {
		return (this.channel == null || this.channel.isClosed());
	}

	@Override
	void cd(String remotePath) throws Exception {
		this.channel.cd(remotePath);
	}

	@SuppressWarnings("unchecked")
	@Override
	List<String> ls(String path) throws Exception {
		List<String> files = new ArrayList<String>();
		Vector<LsEntry> entries = this.channel.ls(path);
		if ( entries != null && !entries.isEmpty() ) {
			for ( LsEntry entry : entries ) {
				files.add(entry.getFilename());
			}
		}
		return files;
	}

	@Override
	void rm(String fileName) throws Exception {
		this.channel.rm(fileName);
	}

	@Override
	void get(String fileName, String absolutePath) throws Exception {
		this.channel.get(fileName, absolutePath);
	}

	@Override
	String pwd() throws Exception {
		return this.channel.pwd();
	}

	@Override
	void put(String localFileName, String dstFileName) throws Exception {
		this.channel.put(localFileName, dstFileName);
	}

	@Override
	Integer getDefaultPort() {
		return 22;
	}

	@Override
	public String getType() {
		return name;
	}

}
