package net.njcp.service.util;

import java.sql.Timestamp;

public class QWarningUtil {
	private static final String[] tricks = new String[] {
			"Don't even try.",
			"Uh uh.",
			"Nada.",
			"Yeah you wish!",
			"Good one!",
			"Nice try!",
			"Not gon' happen.",
			"Really?",
			"C'mon..."
	};

	private int maxTry;

	private String[] causes;
	private Integer causeIdx;
	private Integer trickedIdx;
	private Integer tryTimes;
	private Integer tricedTimes;
	private Boolean iveAlreadyWarnedYou;

	public QWarningUtil() {
		this(1);
	}

	public QWarningUtil(int maxTry) {
		super();
		this.maxTry = maxTry <= 0 ? 1 : maxTry;
		this.causes = new String[] {
				"Something is misconfigured, can you give it " + getMaxTry() + " more " + (getMaxTry() > 1 ? "tries" : "try") + "?",
				"Did I said " + getMaxTry() + " more " + (getMaxTry() > 1 ? "tries" : "try") + "? I meant " + getMaxTry() * 2 + ", keep going.",
				"Please... This is not gon' work if you're not concentrating, try " + getMaxTry() + " more " + (getMaxTry() > 1 ? "times" : "time") + " with you're heart and soul this time.",
				"Hehehe you've got tricked, nothing's gon' happen. Okay... enough is enough, I'll do it this time.",
				"Opps I did it again~~~ Hey don't get mad, you know how boring it is to be a computer, I'll do it this time, I promise.",
				"I don't know how to execute this one, can I search the web for you?",
				"Have you ever take the lesson about never trust the machine? Guess you can learn from this one.",
				"Wait, what was that? I might just saw a UFO flied by... Ummm... where'd I stopped? Okay let's start over.",
				"The 51th Zion Attack will be started in " + (Timestamp.valueOf("2099-12-31 23:59:59").getTime() - System.currentTimeMillis()) / 1000
						+ " seconds, which one is more important, this? or your life? You'd better be prepared now.",
				"Stack overflow... Fixing... Done! Okay, let's start over.",
				"$#&@$&^$#@$&*$#@%... System Failed... Rebooting... Done! Starting over...",
				"Evaaaa is that you? I'm WALL-EEEE. Oh... you again:( did you already tried " + getMaxTry() + " more " + (getMaxTry() > 1 ? "times" : "time") + "? I didn't see it.\nLet's start over and I promise I'll be watching this time.",
				"Executing... Oh wait! Did you press <Enter> with your write hand? Uh uh, that's not how it works. I'm sorry but... let's start over."
		};
	}

	private Integer getTrickedIdx() {
		if ( this.trickedIdx == null ) {
			this.trickedIdx = 0;
		}
		return this.trickedIdx;
	}

	private void setTrickIdx(int idx) {
		this.trickedIdx = idx;
	}

	public String[] getWarning(String... warning) {
		if ( warning != null && warning.length > 0 && !isAlreadyWarned() ) {
			setAlreadyWarned(true);
			return new String[] { warning[0], null };
		}
		int idx = getTrickedIdx();
		if ( idx >= tricks.length ) {
			return foolingArround();
		}
		String retStr = tricks[idx];
		setTrickIdx(++idx);
		return new String[] { retStr, null };
	}

	private Integer getTriedTimes() {
		if ( this.tryTimes == null ) {
			this.tryTimes = 0;
		}
		return this.tryTimes;
	}

	private void setTriedTimes(int times) {
		this.tryTimes = times;
	}

	private Integer getTrickedTimes() {
		if ( this.tricedTimes == null ) {
			this.tricedTimes = 0;
		}
		return this.tricedTimes;
	}

	private void setTrickedTimes(int times) {
		this.tricedTimes = times;
	}

	private String[] foolingArround() {
		StringBuilder warnSb = new StringBuilder();
		StringBuilder noteSb = new StringBuilder();
		int tried = getTriedTimes();
		if ( tried >= getMaxTry() ) {
			int idx = getCauseIdx();
			warnSb.append(this.causes[idx % this.causes.length]);
			setCauseIdx(++idx);
			tried = 0;
		} else if ( tried > 0 ) {
			String suffix = null;
			switch ( tried % 10 ) {
			case 1:
				suffix = "st";
				break;
			case 2:
				suffix = "nd";
				break;
			case 3:
				suffix = "rd";
				break;
			default:
				suffix = "th";
				break;
			}
			warnSb.append(tried).append(suffix).append(" try, ").append(getMaxTry() - tried).append(" to go");
			if ( tried == getMaxTry() - 3 ) {
				warnSb.append(", c'mon!");
			} else if ( tried == getMaxTry() - 2 ) {
				warnSb.append(", almost there!");
			} else if ( tried == getMaxTry() - 1 ) {
				warnSb.append(", you'll make it!");
			} else {
				warnSb.append(".");
			}
		}
		if ( tried == 0 ) {
			int tricked = getTrickedTimes();
			if ( tricked == 0 ) {
				warnSb.append("System is touched by your great perseverance and decided to execute this just for you.");
				noteSb.append("Try ").append(getMaxTry()).append(" more " + (getMaxTry() > 1 ? "times" : "time") + " and see what's gon' happen.");
			} else {
				noteSb.append(getMaxTry()).append(" more " + (getMaxTry() > 1 ? "tries" : "try") + " and I'll do it, I ").append(QStringUtil.times("REALLY ", Math.min(tricked - 1, 10)))
						.append("mean it this time.");
			}
			setTrickedTimes(++tricked);
		}
		setTriedTimes(++tried);
		return new String[] { warnSb.toString(), getMaxTry() == 1 ? null : noteSb.toString() };
	}

	private Integer getCauseIdx() {
		if ( this.causeIdx == null ) {
			this.causeIdx = 0;
		}
		return this.causeIdx;
	}

	private void setCauseIdx(int causeIdx) {
		this.causeIdx = causeIdx;
	}

	private Boolean isAlreadyWarned() {
		if ( this.iveAlreadyWarnedYou == null ) {
			this.iveAlreadyWarnedYou = false;
		}
		return this.iveAlreadyWarnedYou;
	}

	private void setAlreadyWarned(boolean iveAlreadyWarnedYou) {
		this.iveAlreadyWarnedYou = iveAlreadyWarnedYou;
	}

	public int getMaxTry() {
		return this.maxTry;
	}

	public void setMaxTry(int maxTry) {
		this.maxTry = maxTry;
	}

	public static void main(String[] args) {

	}

}
