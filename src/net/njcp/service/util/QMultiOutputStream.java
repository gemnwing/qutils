package net.njcp.service.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class QMultiOutputStream extends OutputStream {
	List<OutputStream> streamList = new ArrayList<OutputStream>();
	private static final PrintStream DEFAULT_SYSTEM_OUT = System.out;
	private static final PrintStream DEFAULT_SYSTEM_ERR = System.err;

	public QMultiOutputStream(Object... params) {
		for ( Object param : params ) {
			appendAStream(param);
		}
	}

	public QMultiOutputStream append(File file) {
		return appendAStream(file);
	}

	public QMultiOutputStream append(String fileName) {
		return appendAStream(fileName);
	}

	public QMultiOutputStream append(OutputStream os) {
		return appendAStream(os);
	}

	private QMultiOutputStream appendAStream(Object param) {
		if ( param != null ) {
			if ( param instanceof OutputStream ) {
				this.streamList.add((OutputStream) param);
			} else {
				File file = null;
				if ( param instanceof File ) {
					file = (File) param;
				} else {
					file = new File(String.valueOf(param));
				}
				try {
					this.streamList.add(new FileOutputStream((File) param));
				} catch ( FileNotFoundException e ) {
					QLog.error("Failed add file \"" + file.getAbsolutePath() + "\" to multi output stream.", e);
				}
			}
		}
		return this;
	}

	public static void makeSystemOutTeeMode(File file) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file);
		} catch ( FileNotFoundException e ) {
			QLog.error("File \"" + file.getAbsolutePath() + "\" not found and cannot be created.");
		}
		System.setOut(new PrintStream(new QMultiOutputStream(DEFAULT_SYSTEM_OUT, fos)));
		System.setErr(new PrintStream(new QMultiOutputStream(DEFAULT_SYSTEM_ERR, fos)));
		QLog.debug("System.out/err has been set to tee mode with file \"" + file.getAbsolutePath() + "\".");
	}

	public static void resetSystemOut() {
		QLog.debug("System.out/err has been set to default.");
		if ( DEFAULT_SYSTEM_OUT != null ) {
			PrintStream curSystemOut = System.out;
			System.setOut(DEFAULT_SYSTEM_OUT);
			curSystemOut.close();
		}
		if ( DEFAULT_SYSTEM_ERR != null ) {
			PrintStream curSystemErr = System.err;
			System.setErr(DEFAULT_SYSTEM_ERR);
			curSystemErr.close();
		}
	}

	@Override
	public void write(int paramInt) throws IOException {
		for ( OutputStream stream : this.streamList ) {
			try {
				stream.write(paramInt);
			} catch ( Throwable t ) {

			}
		}
	}

	@Override
	public void close() {
		synchronized ( this.streamList ) {
			if ( this.streamList.isEmpty() ) {
				return;
			}
			for ( Iterator<OutputStream> iter = this.streamList.iterator(); iter.hasNext(); ) {
				OutputStream stream = iter.next();
				if ( stream.equals(DEFAULT_SYSTEM_OUT) || stream.equals(DEFAULT_SYSTEM_ERR) ) {
					iter.remove();
					continue;
				}
				try {
					stream.close();
					QLog.debug("Stream \"" + stream + "\" closed.");
					iter.remove();
				} catch ( Throwable t ) {

				}
			}
		}
	}

}
