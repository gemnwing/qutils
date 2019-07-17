package net.njcp.service.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import net.njcp.service.util.I18N;

public abstract class QFtpUtil {

	public static final int FTP = 0;
	public static final int SFTP = 1;

	String user;
	String passwd;
	String host;
	Integer port;
	Properties config;

	public static QFtpUtil getInstance(int connType, String host, String user, String passwd) {
		return getInstance(connType, host, user, passwd, null);
	}

	public static QFtpUtil getInstance(int connType, String host, String user, String passwd, Integer port) {
		QFtpUtil util = null;
		String className = null;
		try {
			switch ( connType ) {
			case 0:
				className = "net.njcp.service.util.QFtp";
				util = (QFtpUtil) Class.forName(className).newInstance();
				break;
			case 1:
				className = "net.njcp.service.util.QSFtp";
				util = (QFtpUtil) Class.forName(className).newInstance();
				break;
			default:
				className = "unknown";
				throw new QFtpException(I18N.tr("Unknown FTP connection type, supported type: FTP(0) or SFTP(1)."));
			}
		} catch ( Throwable t ) {
			QLog.error(I18N.tr("Failed to get a instance of class \"{0}\".", className), t);
		}
		if ( util != null ) {
			util.setHost(host);
			util.setUser(user);
			util.setPasswd(passwd);
			util.setPort(port);
		}
		return util;
	}

	public static QFtpUtil getSFtpInstance(String host, String user, String passwd) {
		return getInstance(SFTP, host, user, passwd);
	}

	public static QFtpUtil getFtpInstance(String host, String user, String passwd) {
		return getInstance(FTP, host, user, passwd);
	}

	public static QFtpUtil getSFtpInstance(String host, String user, String passwd, int port) {
		return getInstance(SFTP, host, user, passwd, port);
	}

	public static QFtpUtil getFtpInstance(String host, String user, String passwd, int port) {
		return getInstance(FTP, host, user, passwd, port);
	}

	QFtpUtil() {
	}

	QFtpUtil(String host, String user, String passwd) {
		this(host, user, passwd, null);
	}

	QFtpUtil(String host, String user, String passwd, Integer port) {
		this.user = user;
		this.passwd = passwd;
		this.host = host;
		this.port = port;
	}

	public void put(String remotePath, boolean delAfterPut, File... localFiles) {
		boolean closeAtEnd = false;
		if ( isClosed() ) {
			try {
				open();
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("Failed to open an {0} channel.", getType()), e);
				close();
				return;
			}
			closeAtEnd = true;
		}
		try {
			QLog.debug(I18N.tr("Current directory \"{0}\".", pwd()));
			QLog.info(I18N.tr("Changing directory to \"{0}\" ...", remotePath));
			cd(remotePath);
			QLog.debug(I18N.tr("Current directory \"{0}\".", pwd()));
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

					put(localFileName, dstFileName);
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

	abstract void put(String localFileName, String dstFileName) throws Exception;

	@Deprecated
	public List<File> get(String remotePath, String keyword, String localPath, boolean delAfterGet) {
		return get(remotePath, localPath, delAfterGet, keyword);
	}

	public List<File> get(String remotePath, String localPath, boolean delAfterGet, String... keywords) {
		List<File> retList = new ArrayList<File>();
		boolean closeAtEnd = false;
		if ( isClosed() ) {
			try {
				open();
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("Failed to open an {0} channel.", getType()), e);
				close();
				return retList;
			}
			closeAtEnd = true;
		}
		try {
			QLog.debug(I18N.tr("Current directory \"{0}\".", pwd()));
			QLog.info(I18N.tr("Changing directory to \"{0}\" ...", remotePath));
			cd(remotePath);
			QLog.debug(I18N.tr("Current directory \"{0}\".", pwd()));
		} catch ( Exception e ) {
			QLog.error(I18N.tr("Failed to change to directory \"{0}\".", remotePath), e);
			return retList;
		}

		if ( keywords.length == 0 ) {
			keywords = new String[] { "*" };
		}
		List<String> remoteFiles = new ArrayList<String>();
		for ( String keyword : keywords ) {
			List<String> tmpFiles = null;
			try {
				tmpFiles = ls(keyword);
			} catch ( Throwable e ) {
				QLog.error(I18N.tr("List file with \"{0}\" error.", keyword), e);
			}
			if ( tmpFiles == null || tmpFiles.isEmpty() ) {
				QLog.debug(I18N.tr("No file matching \"{0}\" was found.", keyword));
			} else {
				QLog.debug(I18N.tr("{0} file(s) matching \"{1}\" was found.", tmpFiles.size(), keyword));
				remoteFiles.addAll(tmpFiles);
			}
		}
		if ( remoteFiles == null || remoteFiles.isEmpty() ) {
			QLog.info(I18N.tr("No file was found."));
			return retList;
		} else {
			for ( String fileName : remoteFiles ) {
				if ( localPath == null ) { // List
					File remoteFile = new File(remotePath + "/" + fileName).getAbsoluteFile();
					retList.add(remoteFile);
					continue;
				}
				File localFile = new File(localPath + "/" + fileName).getAbsoluteFile();
				try {
					QLog.info(I18N.tr("Downloading \"{0}\" to \"{1}\"", fileName, localPath));
					get(fileName, localFile.getAbsolutePath());
				} catch ( Exception e ) {
					QLog.error(I18N.tr("Failed to get file \"{0}\".", fileName));
					continue;
				}
				retList.add(localFile);
				if ( delAfterGet ) {
					try {
						rm(remotePath + "/" + fileName);
					} catch ( Exception e ) {
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

	abstract Integer getDefaultPort();

	public abstract String getType();

	abstract void rm(String fileName) throws Exception;

	abstract void get(String fileName, String absolutePath) throws Exception;

	abstract void cd(String remotePath) throws Exception;

	public List<File> ls(String remotePath, String... keywords) {
		return get(remotePath, null, false, keywords);
	}

	abstract List<String> ls(String keyword) throws Exception;

	abstract boolean isClosed();

	abstract String pwd() throws Exception;

	public void open() throws Exception {
		QLog.debug(I18N.tr("Open a {0} connection to server<{1}> port<{2}> user<{3}> pass<{4}>", getType(), getHost(), getPort(), getUser(), showPasswd()));
		connectNLogin();
		QLog.debug(I18N.tr("Connected and logged in to server <{0}>.", getHost()));
	}

	abstract void connectNLogin() throws Exception;

	public void close() {
		try {
			LogoutNdisconnect();
		} catch ( Throwable t ) {
			QLog.error(I18N.tr("Failed to close {0} client.", getType()), t);
		}
	}

	public abstract void LogoutNdisconnect() throws Exception;

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

	public String showPasswd() {
		return QStringUtil.passwd(this.passwd);
	}

	public String getHost() {
		return this.host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public Properties getConfig() {
		return this.config;
	}

	public void setConfig(Properties config) {
		this.config = config;
	}

	public Integer getPort() {
		if ( this.port == null ) {
			this.port = getDefaultPort();
		}
		return this.port;
	}

	public void setPort(Integer port) {
		this.port = port;
	}

	public static void main(String[] args) {
		QFtpUtil ftp;
		ftp = getInstance(SFTP, "192.1.105.17", "root", I18N.tr("root.2016"), 22);
		// ftp = getInstance(FTP, "26.47.30.11", "d5000", "d5000");
		try {
			ftp.open();
		} catch ( Exception e ) {
			e.printStackTrace();
		}
		// ftp.get("/home/d5000/shandong/var/eedas_source/files", "*.JSON", "/Users/Dominic/Downloads/NOBKP", false);
		// File[] files = new File("/Users/Dominic/Downloads/NOBKP").listFiles(new FilenameFilter() {
		// @Override
		// public boolean accept(File dir, String name) {
		// if ( name.toLowerCase().matches("^.*.json$") ) {
		// return true;
		// }
		// return false;
		// }
		// });

		List<File> files1;
		// files1 = ftp.get("/home/d5000/var/log", "/Users/Dominic/Downloads/NOBKP/ftptest", false, "rtdb*log");
		// files1 = ftp.ls("/home/d5000", "*osp*");
		files1 = ftp.ls("/root", "*");
		QLog.println(files1);
		ftp.close();
	}

}
