package net.njcp.service.util;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;

public class QSFtp {

	private String user;
	private String passwd;
	private String host;
	private Integer port;

	private Session session;
	private ChannelSftp channel;
	private Properties config;

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

	public QSFtp(String host, Integer port, String user, String passwd) {
		super();
		this.user = user;
		this.passwd = passwd;
		this.host = host;
		this.port = port;
	}

	public QSFtp(String host, String user, String passwd) {
		super();
		this.user = user;
		this.passwd = passwd;
		this.host = host;
		this.port = 22;
	}

	public void put(String remotePath, boolean delAfterPut, File... localFiles) {
		boolean closeAtEnd = false;
		if ( this.channel == null || this.channel.isClosed() ) {
			try {
				open();
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("Failed to open an SFTP channel."), e);
				close();
				return;
			}
			closeAtEnd = true;
		}
		try {
			QLog.debug(I18N.tr("Current directory \"{0}\".", this.channel.pwd()));
			QLog.info(I18N.tr("Changing directory to \"{0}\" ...", remotePath));
			this.channel.cd(remotePath);
			QLog.debug(I18N.tr("Current directory \"{0}\".", this.channel.pwd()));
		} catch ( Exception e ) {
			QLog.error(I18N.tr("Failed to change to directory \"{0}\".", remotePath), e);
			return;
		}
		for ( File file : localFiles ) {
			String localFileName = file.getAbsolutePath();
			if ( file.isFile() ) {
				String dstFileName = file.getName();
				try {
					QLog.debug(I18N.tr("Uploading file \"{0}\" ...", localFileName));

					this.channel.put(localFileName, dstFileName);
					// this.channel.rename(dstFileName + "~", dstFileName);

				} catch ( Exception e ) {
					QLog.error(I18N.tr("Failed to upload file \"{0}\".", localFileName), e);
					continue;
				}
				QLog.info(I18N.tr("File \"{0}\" uploaded.", localFileName));
				if ( delAfterPut ) {
					if ( !file.delete() ) {
						QLog.error(I18N.tr("Failed to remove local file \"{0}\".", localFileName));
					}
				}
			} else {
				QLog.error(file.exists() ? (I18N.tr("\"{0}\" is a directory.", localFileName)) : (I18N.tr("No such file \"{0}\".", localFileName)));
				continue;
			}
		}
		if ( closeAtEnd ) {
			close();
		}
	}

	@Deprecated
	public List<File> get(String remotePath, String keyword, String localPath, boolean delAfterGet) {
		return get(remotePath, localPath, delAfterGet, keyword);
	}

	public List<File> ls(String remotePath, String... keywords) {
		return get(remotePath, null, false, keywords);
	}

	@SuppressWarnings("unchecked")
	public List<File> get(String remotePath, String localPath, boolean delAfterGet, String... keywords) {
		List<File> retList = new ArrayList<File>();
		boolean closeAtEnd = false;
		if ( this.channel == null || this.channel.isClosed() ) {
			try {
				open();
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("Failed to open an SFTP channel."), e);
				close();
				return retList;
			}
			closeAtEnd = true;
		}
		try {
			QLog.debug(I18N.tr("Current directory \"{0}\".", this.channel.pwd()));
			QLog.info(I18N.tr("Changing directory to \"{0}\" ...", remotePath));
			this.channel.cd(remotePath);
			QLog.debug(I18N.tr("Current directory \"{0}\".", this.channel.pwd()));
		} catch ( Exception e ) {
			QLog.error(I18N.tr("Failed to change to directory \"{0}\".", remotePath), e);
			return retList;
		}

		if ( keywords.length == 0 ) {
			keywords = new String[] { "*" };
		}
		Vector<LsEntry> entries = new Vector<LsEntry>();
		for ( String keyword : keywords ) {
			Vector<LsEntry> tmpEntry = null;
			try {
				tmpEntry = this.channel.ls(keyword);
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("List file with \"{0}\" error.", keyword), e);
			}
			if ( tmpEntry == null || tmpEntry.isEmpty() ) {
				QLog.debug(I18N.tr("No file matching \"{0}\" was found.", keyword));
			} else {
				entries.addAll(tmpEntry);
			}
		}
		if ( entries == null || entries.isEmpty() ) {
			QLog.info(I18N.tr("No file was found."));
			return retList;
		} else {
			for ( LsEntry entry : entries ) {
				String fileName = entry.getFilename();
				if ( localPath == null ) { // List
					File remoteFile = new File(remotePath + "/" + fileName).getAbsoluteFile();
					retList.add(remoteFile);
					continue;
				}
				QLog.info(I18N.tr("File will be downloaded to \"{0}\".", localPath));
				File localFile = new File(localPath + "/" + fileName).getAbsoluteFile();
				try {
					QLog.info(I18N.tr("Downloading \"{0}\" ...", fileName));
					this.channel.get(fileName, localFile.getAbsolutePath());
				} catch ( SftpException e ) {
					QLog.error(I18N.tr("Failed to get file \"{0}\".", fileName));
					continue;
				}
				retList.add(localFile);
				if ( delAfterGet ) {
					try {
						this.channel.rm(remotePath + "/" + fileName);
					} catch ( SftpException e ) {
						QLog.error(I18N.tr("Failed to remove file \"{0}/{1}\"", remotePath, fileName));
					}
				}
			}
		}

		if ( closeAtEnd ) {
			close();
		}
		return retList;
	}

	public void open() throws Exception {
		QLog.debug(I18N.tr("Connecting to server<{0}> port<{1}> user<{2}> pass<{3}>", this.host, this.port, this.user, QStringUtil.passwd(this.passwd)));

		this.session = new JSch().getSession(getUser(), getHost(), getPort());
		if ( this.session == null ) {
			throw new QSFtpException(I18N.tr("Session is null."));
		}
		this.session.setPassword(getPasswd());
		this.session.setConfig(DEFAULT_CONFIG);
		if ( getConfig() != null ) {
			this.session.setConfig(getConfig());
		}
		this.session.connect();
		this.channel = (ChannelSftp) this.session.openChannel("sftp");
		if ( this.channel == null ) {
			throw new QSFtpException(I18N.tr("SFTP Channel is null."));
		}
		this.channel.connect();
		QLog.debug(I18N.tr("Connected and logged in to server <{0}>.", this.host));
	}

	public void close() {
		if ( this.channel != null ) {
			this.channel.disconnect();
		}
		if ( this.session != null ) {
			this.session.disconnect();
		}
	}

	public String getUser() {
		return this.user;
	}

	public void setUser(String user) {
		this.user = user;
	}

	public String getPasswd() {
		return this.passwd;
	}

	public void setPasswd(String passwd) {
		this.passwd = passwd;
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Integer getPort() {
		if ( this.port == null ) {
			this.port = 22;
		}
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public Session getSession() {
		return this.session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Properties getConfig() {
		return this.config;
	}

	public void setConfig(Properties config) {
		this.config = config;
	}

	public ChannelSftp getChannel() {
		return this.channel;
	}

	public void setChannel(ChannelSftp channel) {
		this.channel = channel;
	}

	@SuppressWarnings("unused")
	public static void main(String[] args) {
		QSFtp ftp = new QSFtp("10.144.119.203", "d5000", "d5000.2017");
		try {
			ftp.open();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		// ftp.get("/home/d5000/shandong/var/eedas_source/files", "*.JSON", "/Users/Dominic/Downloads/NOBKP", false);
		File[] files = new File("/Users/Dominic/Downloads/NOBKP").listFiles(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				if ( name.toLowerCase().matches("^.*.json$") ) {
					return true;
				}
				return false;
			}
		});

		List<File> files1 = ftp.ls("/home/d5000/shandong/var/eedas_source/files", "*");
		QLog.println(files1);
		ftp.close();
	}
}
