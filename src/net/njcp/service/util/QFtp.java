package net.njcp.service.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import net.njcp.service.util.I18N;

public class QFtp extends QFtpUtil {

	static final String name = "FTP";

	private FTPClient ftpClient;

	QFtp() {
		super();
	}

	public QFtp(String host, String user, String passwd) {
		super(host, user, passwd);
	}

	public QFtp(String host, String user, String passwd, int port) {
		super(host, user, passwd, port);
	}

	@Override
	public void connectNLogin() throws Exception {
		if ( this.ftpClient == null ) {
			this.ftpClient = new FTPClient();
		}
		this.ftpClient.connect(getHost(), getPort());
		if ( this.ftpClient.getReplyCode() != FTPReply.SERVICE_READY ) {
			throw new QFtpException(I18N.tr("Failed to connect to {0}:{1}.", getHost(), getPort()));
		}
		this.ftpClient.login(getUser(), getPasswd());
		if ( this.ftpClient.getReplyCode() != FTPReply.USER_LOGGED_IN ) {
			throw new QFtpException(I18N.tr("Login incorrect."));
		}
		this.ftpClient.enterLocalPassiveMode();
		this.ftpClient.setKeepAlive(true);
		this.ftpClient.setBufferSize(1024 * 1024);
		this.ftpClient.setFileType(org.apache.commons.net.ftp.FTP.BINARY_FILE_TYPE);
	}

	@Override
	public void LogoutNdisconnect() throws Exception {
		if ( this.ftpClient != null ) {
			this.ftpClient.logout();
			this.ftpClient.disconnect();
		}
	}

	public FTPClient getFtpClient() {
		return this.ftpClient;
	}

	public void setFtpClient(FTPClient ftpClient) {
		this.ftpClient = ftpClient;
	}

	@Override
	boolean isClosed() {
		return (this.ftpClient == null || !this.ftpClient.isConnected());
	}

	@Override
	void cd(String remotePath) throws Exception {
		this.ftpClient.changeWorkingDirectory(remotePath);
		if ( this.ftpClient.getReplyCode() != FTPReply.FILE_ACTION_OK ) {
			throw new QFtpException(I18N.tr("Failed to change working directory to \"{0}\".", remotePath));
		}
	}

	@Override
	void put(String src, String dst) throws Exception {
		File localFile = new File(src);
		FileInputStream fis = new FileInputStream(localFile);
		boolean success = this.ftpClient.storeFile(dst, fis);
		fis.close();
		if ( !success ) {
			throw new QFtpException(I18N.tr("Ftp reply false while uploading."));
		}
	}

	@Override
	void rm(String fileName) throws Exception {
		this.ftpClient.deleteFile(fileName);
	}

	@Override
	void get(String src, String dst) throws Exception {
		File localFile = new File(dst);
		FileOutputStream fos = new FileOutputStream(localFile);
		boolean success = this.ftpClient.retrieveFile(src, fos);
		fos.close();
		if ( !success ) {
			throw new QFtpException(I18N.tr("Ftp reply false while downloading."));
		}
	}

	@Override
	List<String> ls(String keyword) throws Exception {
		List<String> retList = new ArrayList<String>();
		FTPFile[] files = this.ftpClient.listFiles(keyword);
		if ( files != null && files.length != 0 ) {
			for ( FTPFile file : files ) {
				retList.add(file.getName());
			}
		}
		return retList;
	}

	@Override
	String pwd() throws Exception {
		String retStr = this.ftpClient.printWorkingDirectory();
		return retStr;
	}

	@Override
	Integer getDefaultPort() {
		return 21;
	}

	@Override
	public String getType() {
		return name;
	}
}
