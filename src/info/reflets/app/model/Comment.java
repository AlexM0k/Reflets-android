package info.reflets.app.model;

import info.reflets.app.utils.Tools;

import java.util.Calendar;

import android.text.format.DateFormat;

public class Comment {

	String mTitle;
	String mLink;
	String mAuthor;
	Calendar mDate;
	String mDateStr;
	String mDescription;
	String mContent;
	
	public String getTitle() {
		return mTitle;
	}
	public void setTitle(String mTitle) {
		this.mTitle = mTitle;
	}
	public String getLink() {
		return mLink;
	}
	public void setLink(String mLink) {
		this.mLink = mLink;
	}
	public String getAuthor() {
		return mAuthor;
	}
	public void setAuthor(String mAuthor) {
		this.mAuthor = mAuthor;
	}
	public String getDate() {
		return mDateStr;
	}
	public void setDate(String date) {
		this.mDate = Tools.parseDate(date);
		mDateStr = DateFormat.format("dd/MM/yyyy hh:mm", this.mDate).toString();
	}
	public String getDescription() {
		return mDescription;
	}
	public void setDescription(String mDescription) {
		this.mDescription = mDescription;
	}
	public String getContent() {
		return mContent;
	}
	public void setContent(String mContent) {
		this.mContent = mContent;
	}
	
	/*
	 * 
	 
	 <title>Par : Vico</title>
		<link>http://reflets.info/opsyria-s04e01-the-iron-strike/#comment-24181</link>
		<dc:creator>Vico</dc:creator>

		<pubDate>Mon, 27 Feb 2012 20:58:22 +0000</pubDate>
		<guid isPermaLink="false">http://reflets.info/?p=15850#comment-24181</guid>
		<description>Petite question également ;-)
La réponse à l&#039;option --tcp-timestamp, est renvoyé par l&#039;équipement lui-même?</description>
		<content:encoded><![CDATA[<p>Petite question également <img src='http://reflets.info/wp-includes/images/smilies/icon_wink.gif' alt=';-)' class='wp-smiley' /><br />
La réponse à l&#8217;option &#8211;tcp-timestamp, est renvoyé par l&#8217;équipement lui-même?</p>
]]></content:encoded>

	 
	 */
}
