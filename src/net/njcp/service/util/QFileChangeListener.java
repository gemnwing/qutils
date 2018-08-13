package net.njcp.service.util;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public abstract class QFileChangeListener {
	List<FileTarget> targets;
	final Object lock = new Object();

	public QFileChangeListener(File... files) {
		super();
		this.targets = new ArrayList<FileTarget>();
		for ( File file : files ) {
			if ( file != null ) {
				this.targets.add(new FileTarget(file));

			}
		}
	}

	static class FileTarget {
		enum FileState {
			NOCHANGE, CREATED, MISSED, MODIFIED
		};

		File file;
		Long lastModificationTime;
		FileState state;

		public FileTarget(File file) {
			this.file = file;
			if ( file.exists() ) {
				this.lastModificationTime = file.lastModified();
			}
		}

		public File getFile() {
			return this.file;
		}

		public void setFile(File file) {
			this.file = file;
		}

		public Long getLastModificationTime() {
			return this.lastModificationTime;
		}

		public void setLastModificationTime(Long lastModificationTime) {
			this.lastModificationTime = lastModificationTime;
		}
	}

	public void check() {
		synchronized ( this.lock ) {
			for ( FileTarget target : this.targets ) {
				File file = target.getFile();
				if ( file.exists() ) {
					if ( file.lastModified() != target.getLastModificationTime() ) {

					}
				} else {
					// file removed
					target.setLastModificationTime(null);
				}
			}
		}
	}
}
