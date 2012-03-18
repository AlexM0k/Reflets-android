package info.reflets.app.model;

import info.reflets.app.utils.Tools;

import java.util.Calendar;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.format.DateFormat;

/***
 * Model of an article 
 *
 */
public class Article implements Parcelable {

	String 		mTitle;
	String 		mLink;
	Calendar	mDate;
	String		mDateStr;
	String 		mDescription;
	String		mAuthor;
	String		mContent;
	String 		mImage;
	String		mCommentUrl;
	
	public Article(){
	}
	
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
	public String getDate() {
		return mDateStr;
	}
	public void setDate(String date) {
		this.mDate = Tools.parseDate(date);
		mDateStr = DateFormat.format("dd/MM/yyyy", this.mDate).toString();
	}
	public String getDescription() {
		return mDescription.trim();
	}
	public void setDescription(String mDescription) {
		this.mDescription = mDescription;
	}
	public String getAuthor() {
		return mAuthor;
	}
	public void setAuthor(String mAuthor) {
		this.mAuthor = mAuthor;
	}
	public String getContent() {
		return mContent;
	}
	public void setContent(String mContent) {
		this.mContent = mContent;
	}
	public String getImage() {
		return mImage;
	}
	public void setImage(String mImage) {
		this.mImage = mImage;
	}
	public String getShareDescription(){
		return this.mTitle + " " + this.mLink;
	}
	
	public String getCommentUrl() {
		return mCommentUrl;
	}

	public void setCommentUrll(String mCommentUrl) {
		this.mCommentUrl = mCommentUrl;
	}

	public int describeContents() {
		return 0;
	}
	
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mTitle);
		dest.writeString(mLink);
		dest.writeLong(mDate.getTimeInMillis());
		dest.writeString(mDateStr);
		dest.writeString(mDescription);
		dest.writeString(mAuthor);
		dest.writeString(mContent);	
		dest.writeString(mImage);	
		dest.writeString(mCommentUrl);
	}
	
	public Article(Parcel in){
		mTitle 	= in.readString();
		mLink 	= in.readString();
		mDate 	= Calendar.getInstance();
		mDate.setTimeInMillis(in.readLong());
		
		mDateStr 		= in.readString();
		mDescription 	= in.readString();
		mAuthor			= in.readString();
		mContent		= in.readString();
		mImage			= in.readString();
		mCommentUrl		= in.readString();
	}
	
	public static final Parcelable.Creator<Article> CREATOR = new Parcelable.Creator<Article>() {
		public Article createFromParcel(Parcel in) {
		    return new Article(in);
		}
		
		public Article[] newArray(int size) {
		    return new Article[size];
		}
	};
}
