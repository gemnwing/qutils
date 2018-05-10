package net.njcp.service.util;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

public class I18NGUI {
	private JFrame i18nPatchTool;
	// private boolean running = false;
	private final ButtonGroup buttonGroup = new ButtonGroup();
	private Action updateAction;
	private Action patchAction;
	private JRadioButton radioButtonPatch;
	private JRadioButton radioButtonUpdate;
	private MyArea prompt;
	private JPanel promptPanel;
	private JPanel mainPanel;
	private Color defaultPromptFontColor = new Color(160, 160, 160);
	private StringBuilder sb;
	private List<String> logs;
	private boolean processing = false;
	private boolean finished = false;
	private JLabel logLabel;
	private JLabel aotLabel;
	private static final String TEXT_PREFIX = "<html>";
	private static final String TEXT_SUFFIX = "</html>";
	private static final String NEW_LINE = "<br>";
	protected static final String MONO_SPACE_FONT = I18N.tr("ReplacableMonospaceFont");
	private static final String DEFAULT_FONT = I18N.tr("ReplacableFont");
	private static final String PATCH_PROMPT = TEXT_PREFIX + I18N.tr("Drag files or folders here to patch external string with translation method.") + TEXT_SUFFIX;
	private static final String UPDATE_PROMPT = TEXT_PREFIX + I18N.tr("Drag files or folders here to update relevant translation property files.") + TEXT_SUFFIX;
	private static final Font PROMPT_FONT = new Font(DEFAULT_FONT, Font.PLAIN, 30);

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					I18NGUI window = new I18NGUI();
					window.i18nPatchTool.setVisible(true);
					// Console console = new Console(window.frame, window.textArea);

				} catch ( Throwable t ) {
					t.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the application.
	 */
	public I18NGUI() {
		initialize();
	}

	private void initialize() {
		this.i18nPatchTool = new JFrame();
		// this.i18nPatchTool.setIconImage(Toolkit.getDefaultToolkit().getImage("/Users/Dominic/Documents/Photos/images.png"));
		this.i18nPatchTool.addWindowFocusListener(new WindowFocusListener() {
			@Override
			public void windowGainedFocus(WindowEvent e) {
				// resetPrompt();
			}

			@Override
			public void windowLostFocus(WindowEvent e) {
				// resetPrompt();
			}

		});

		this.i18nPatchTool.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				startOver();
			}
		});

		// this.frmIntrinPatchTool.setResizable(false);
		this.i18nPatchTool.setTitle(I18N.tr("I18N Patch Tool"));
		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();

		Dimension d = new Dimension(380, 300);

		int x = (int) ((s.getWidth() - d.getWidth()) / 2);
		int y = (int) ((s.getHeight() - d.getHeight()) / 2);
		this.i18nPatchTool.setBounds(x, y, (int) d.getWidth(), (int) d.getHeight());
		this.i18nPatchTool.setSize(d);
		this.i18nPatchTool.setMinimumSize(d);
		this.i18nPatchTool.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		this.i18nPatchTool.getContentPane().setLayout(new BorderLayout(0, 0));

		JPanel bgPanel = new JPanel();
		this.i18nPatchTool.getContentPane().add(bgPanel, BorderLayout.SOUTH);
		bgPanel.setLayout(new BorderLayout(0, 0));

		JPanel panel = new JPanel();
		bgPanel.add(panel, BorderLayout.CENTER);

		JLabel bottomGap = new JLabel("");
		bottomGap.setEnabled(false);
		bottomGap.setBorder(new EmptyBorder(20, 0, 0, 0));
		bgPanel.add(bottomGap, BorderLayout.SOUTH);

		this.mainPanel = new JPanel();
		this.i18nPatchTool.getContentPane().add(this.mainPanel, BorderLayout.CENTER);
		this.mainPanel.setLayout(new BorderLayout(0, 0));

		this.promptPanel = new JPanel();
		this.promptPanel.setBackground(getPromptBGColor());
		this.promptPanel.setLayout(new BorderLayout(0, 0));
		this.mainPanel.add(this.promptPanel, BorderLayout.CENTER);
		this.promptPanel.addMouseListener(new MouseAdapter() {
			Color oldFgColor;
			Font oldFont;
			String oldText;
			int oldHAlignment;
			int oldVAlignment;

			@Override
			public void mouseEntered(MouseEvent paramMouseEvent) {
				if ( !I18NGUI.this.finished ) {
					return;
				}
				this.oldFgColor = I18NGUI.this.prompt.getForeground();
				this.oldFont = I18NGUI.this.prompt.getFont();
				this.oldText = I18NGUI.this.prompt.getText();
				this.oldHAlignment = I18NGUI.this.prompt.getHorizontalAlignment();
				this.oldVAlignment = I18NGUI.this.prompt.getVerticalAlignment();

				changeBGColor();
				I18NGUI.this.prompt.setForeground(I18NGUI.this.defaultPromptFontColor);
				I18NGUI.this.prompt.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 24));
				I18NGUI.this.prompt.setText(I18N.tr("Click here to start over."));
				I18NGUI.this.prompt.setHorizontalAlignment(SwingConstants.CENTER);
				I18NGUI.this.prompt.setVerticalAlignment(SwingConstants.CENTER);
			}

			@Override
			public void mouseExited(MouseEvent paramMouseEvent) {
				if ( !I18NGUI.this.finished ) {
					return;
				}
				changeBGColorBack();
				I18NGUI.this.prompt.setForeground(this.oldFgColor);
				I18NGUI.this.prompt.setFont(this.oldFont);
				I18NGUI.this.prompt.setText(this.oldText);
				I18NGUI.this.prompt.setHorizontalAlignment(this.oldHAlignment);
				I18NGUI.this.prompt.setVerticalAlignment(this.oldVAlignment);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				startOver();
			}
		});

		this.prompt = new MyArea(PATCH_PROMPT);
		this.promptPanel.add(this.prompt, BorderLayout.CENTER);

		this.aotLabel = new JLabel("");
		this.mainPanel.add(this.aotLabel, BorderLayout.NORTH);
		this.aotLabel.setBorder(new EmptyBorder(8, 0, 8, 6));
		this.aotLabel.setText(I18N.tr("AOT"));
		this.aotLabel.setHorizontalAlignment(SwingConstants.RIGHT);
		this.aotLabel.setVerticalAlignment(SwingConstants.CENTER);
		this.aotLabel.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 14));
		// this.aotLabel.setForeground(Color.red);
		if ( setAlwaysOnTop(true) ) {
			this.aotLabel.setForeground(Color.LIGHT_GRAY);
		}
		this.aotLabel.addMouseListener(new MouseAdapter() {
			Color oldFgColor;

			@Override
			public void mouseEntered(MouseEvent paramMouseEvent) {
				this.oldFgColor = I18NGUI.this.aotLabel.getForeground();
				I18NGUI.this.aotLabel.setForeground(Color.GRAY);
			}

			@Override
			public void mouseExited(MouseEvent paramMouseEvent) {
				I18NGUI.this.aotLabel.setForeground(this.oldFgColor);
			}

			@Override
			public void mouseClicked(MouseEvent e) {
				if ( setAlwaysOnTop(!I18NGUI.this.i18nPatchTool.isAlwaysOnTop()) ) {
					this.oldFgColor = Color.LIGHT_GRAY;
				} else {
					this.oldFgColor = getPromptBGColor();
				}
				I18NGUI.this.aotLabel.setForeground(this.oldFgColor);
			}
		});

		this.logLabel = new JLabel(" ");
		this.logLabel.setEnabled(false);
		this.mainPanel.add(this.logLabel, BorderLayout.SOUTH);
		this.logLabel.setBorder(new EmptyBorder(8, 0, 8, 0));
		this.logLabel.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 14));
		this.logLabel.setHorizontalAlignment(SwingConstants.CENTER);
		this.logLabel.setVerticalAlignment(SwingConstants.CENTER);

		this.logLabel.addMouseListener(new MouseAdapter() {
			Color oldFgColor;
			String oldText;

			@Override
			public void mouseEntered(MouseEvent paramMouseEvent) {
				if ( !I18NGUI.this.finished ) {
					return;
				}
				this.oldFgColor = I18NGUI.this.logLabel.getForeground();
				this.oldText = I18NGUI.this.logLabel.getText();
				I18NGUI.this.logLabel.setForeground(Color.DARK_GRAY);
				I18NGUI.this.logLabel.setText(I18N.tr("Click here to view the log."));
			}

			@Override
			public void mouseExited(MouseEvent paramMouseEvent) {
				if ( !I18NGUI.this.finished ) {
					return;
				}
				I18NGUI.this.logLabel.setForeground(this.oldFgColor);
				I18NGUI.this.logLabel.setText(this.oldText);
			}

			@Override
			public void mouseClicked(MouseEvent paramMouseEvent) {
				if ( !I18NGUI.this.finished || I18NGUI.this.logs == null || I18NGUI.this.logs.isEmpty() ) {
					return;
				}

				String content = "";
				for ( String oneline : I18NGUI.this.logs ) {
					content += oneline + "\n";
				}

				JFrame logFrame = new JFrame();
				logFrame.setTitle(I18N.tr("Log"));
				Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
				Dimension d = new Dimension(1000, 500);
				int x = (int) ((s.getWidth() - d.getWidth()) / 2);
				int y = (int) ((s.getHeight() - d.getHeight()) / 2);
				logFrame.setBounds(x, y, (int) d.getWidth(), (int) d.getHeight());
				logFrame.setSize(d);
				logFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);

				logFrame.getContentPane().setLayout(new BorderLayout());
				JTextArea logArea = new JTextArea();
				logFrame.getContentPane().add(logArea);
				logArea.setBorder(new EmptyBorder(10, 10, 10, 10));
				logArea.setText(content);
				Color bgColor = new Color(248, 248, 248);
				logArea.setBackground(bgColor);
				// logArea.setLineWrap(true);
				// logArea.setWrapStyleWord(true);
				logArea.setEditable(false);
				logArea.setAutoscrolls(true);
				logArea.setFont(new Font(MONO_SPACE_FONT, Font.PLAIN, 12));
				logArea.setForeground(new Color(40, 40, 40));
				JScrollPane scrollPane = new JScrollPane(logArea);
				scrollPane.setBackground(bgColor);
				logFrame.getContentPane().add(scrollPane, BorderLayout.CENTER);
				logFrame.setVisible(true);

				// clearLog();
			}

		});

		JLabel wGap = new JLabel("");
		wGap.setEnabled(false);
		this.mainPanel.add(wGap, BorderLayout.WEST);
		wGap.setBorder(new EmptyBorder(0, 30, 0, 0));

		JLabel eGap = new JLabel("");
		eGap.setEnabled(false);
		this.mainPanel.add(eGap, BorderLayout.EAST);
		eGap.setBorder(new EmptyBorder(0, 30, 0, 0));

		this.updateAction = new UpdateAction();
		this.patchAction = new PatchAction();
		panel.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		this.radioButtonPatch = new JRadioButton();
		panel.add(this.radioButtonPatch);
		this.radioButtonPatch.setAction(this.patchAction);
		this.radioButtonPatch.setForeground(Color.DARK_GRAY);
		this.radioButtonPatch.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 16));
		this.radioButtonPatch.setVerticalAlignment(SwingConstants.CENTER);
		this.radioButtonPatch.setHorizontalAlignment(SwingConstants.LEFT);
		this.radioButtonPatch.setSelected(true);
		this.buttonGroup.add(this.radioButtonPatch);

		this.radioButtonUpdate = new JRadioButton();
		panel.add(this.radioButtonUpdate);
		this.radioButtonUpdate.setAction(this.updateAction);
		this.radioButtonUpdate.setForeground(Color.DARK_GRAY);
		this.radioButtonUpdate.setFont(new Font(DEFAULT_FONT, Font.PLAIN, 16));
		this.radioButtonUpdate.setHorizontalAlignment(SwingConstants.LEFT);
		this.radioButtonUpdate.setVerticalAlignment(SwingConstants.CENTER);
		this.buttonGroup.add(this.radioButtonUpdate);

		// this.frmIntrinPatchTool.getContentPane().setDropTarget(new DropTarget() {
		// @SuppressWarnings("unchecked")
		// @Override
		// public synchronized void drop(DropTargetDropEvent evt) {
		// try {
		// evt.acceptDrop(DnDConstants.ACTION_COPY);
		// List<File> droppedFiles = (List<File>) evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
		// processFile(droppedFiles);
		// } catch ( Exception ex ) {
		// ex.printStackTrace();
		// }
		// }
		// });

	}

	private boolean setAlwaysOnTop(boolean alwaysOnTop) {
		if ( this.i18nPatchTool.isAlwaysOnTopSupported() ) {
			this.i18nPatchTool.setAlwaysOnTop(alwaysOnTop);
		}
		return this.i18nPatchTool.isAlwaysOnTop();
	}

	private void clearLog() {
		this.logs = null;
		this.logLabel.setText("");
	}

	private void changeBGColor() {
		this.promptPanel.setBackground(getHighlightedPromptBGColor());
	}

	private Color getHighlightedPromptBGColor() {
		int darkenDegree = 24;
		int R = getPromptBGColor().getRed() - darkenDegree;
		int G = getPromptBGColor().getGreen() - darkenDegree;
		int B = getPromptBGColor().getBlue() - darkenDegree;
		return new Color(R, G, B);
	}

	private void changeBGColorBack() {
		this.promptPanel.setBackground(getPromptBGColor());
	}

	private Color getPromptBGColor() {
		return this.mainPanel.getBackground();
	}

	@SuppressWarnings("serial")
	private class MyArea extends JLabel implements DropTargetListener {

		public MyArea(String paramString) {
			super(paramString);
			new DropTarget(this, DnDConstants.ACTION_COPY_OR_MOVE, this);

			// prompt.setHorizontalAlignment(SwingConstants.CENTER);
			// t l b r
			setBorder(new EmptyBorder(0, 10, 0, 10));
			resetPromptFormat(this);

		}

		@Override
		public void dragEnter(DropTargetDragEvent e) {
			startOver();
			changeBGColor();
		}

		@Override
		public void dragExit(DropTargetEvent paramDropTargetEvent) {
			changeBGColorBack();
		}

		@Override
		public void dragOver(DropTargetDragEvent paramDropTargetDragEvent) {
			// QLog.println("Drag over");
		}

		@SuppressWarnings("unchecked")
		@Override
		public void drop(DropTargetDropEvent e) {
			changeBGColorBack();
			try {
				Transferable t = e.getTransferable();

				if ( e.isDataFlavorSupported(DataFlavor.javaFileListFlavor) ) {
					e.acceptDrop(e.getDropAction());

					List<File> files;
					files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);

					if ( files != null && !files.isEmpty() ) {
						I18NGUI.this.processing = true;
						I18NGUI.this.prompt.setForeground(Color.DARK_GRAY);
						I18NGUI.this.prompt.setFont(new Font(MONO_SPACE_FONT, Font.PLAIN, 15));
						I18NGUI.this.prompt.setVerticalAlignment(SwingConstants.TOP);
						I18NGUI.this.prompt.setHorizontalAlignment(SwingConstants.LEADING);
						I18NGUI.this.sb = new StringBuilder();
						QLog.setBufferFlag(true);
						for ( final File file : files ) {
							I18NGUI.this.sb.append(I18N.tr("√ " + file.getName() + NEW_LINE));
							if ( I18NGUI.this.radioButtonPatch.isSelected() ) {
								I18N.patch(file);
							} else {
								I18N.update(file);
							}
						}
						I18NGUI.this.logs = QLog.getLogsFromBuffer();
						if ( QLog.getWarnNErrorsFromBuffer().size() > 0 ) {
							I18NGUI.this.logLabel.setForeground(Color.RED);
							I18NGUI.this.logLabel.setText(I18N.tr("Something's wrong, click here to view the log."));
						} else {
							I18NGUI.this.logLabel.setForeground(Color.LIGHT_GRAY);
							I18NGUI.this.logLabel.setText(I18N.tr("Click here to view the log."));
						}
						QLog.setBufferFlag(false);
						I18NGUI.this.sb.append(NEW_LINE + I18N.tr("Done :)"));
						I18NGUI.this.prompt.setText(TEXT_PREFIX + I18NGUI.this.sb.toString() + TEXT_SUFFIX);
						I18NGUI.this.processing = false;
						I18NGUI.this.finished = true;
					}
					e.dropComplete(true);
				} else
					e.rejectDrop();
			} catch ( Throwable t ) {
			}
		}

		@Override
		public void dropActionChanged(DropTargetDragEvent paramDropTargetDragEvent) {
			// QLog.println("Drop action changed");
		}

	}

	// private boolean isRunning() {
	// return this.running;
	// }
	//
	// private void setRunning(boolean running) {
	// this.running = running;
	// }

	private void resetPromptFormat(MyArea prompt) {
		prompt.setForeground(this.defaultPromptFontColor);
		prompt.setFont(PROMPT_FONT);
		prompt.setVerticalAlignment(SwingConstants.CENTER);
		prompt.setHorizontalAlignment(SwingConstants.CENTER);
		changeBGColorBack();
	}

	private void startOver() {
		if ( this.processing ) {
			return;
		}
		resetPromptFormat(this.prompt);
		resetPromptContent();
		clearLog();
		this.finished = false;
	}

	private void resetPromptContent() {
		if ( this.radioButtonPatch.isSelected() ) {
			this.prompt.setText(PATCH_PROMPT);
		} else {
			this.prompt.setText(UPDATE_PROMPT);
		}
	}

	@SuppressWarnings("serial")
	private class UpdateAction extends AbstractAction {
		public UpdateAction() {
			putValue(NAME, I18N.tr("Update"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			startOver();
		}
	}

	@SuppressWarnings("serial")
	private class PatchAction extends AbstractAction {
		public PatchAction() {
			putValue(NAME, I18N.tr("Patch"));
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			startOver();
		}
	}
}
