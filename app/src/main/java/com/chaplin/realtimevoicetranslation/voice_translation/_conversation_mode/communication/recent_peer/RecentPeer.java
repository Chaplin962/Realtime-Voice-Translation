

package com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode.communication.recent_peer;

import android.bluetooth.BluetoothDevice;
import android.graphics.Bitmap;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.util.Objects;
import com.chaplin.realtimevoicetranslation.tools.gui.peers.GuiPeer;
import com.chaplin.realtimevoicetranslation.tools.gui.peers.Listable;
import com.chaplin.realtimevoicetranslation.bluetooth.Peer;


public class RecentPeer implements Parcelable, Listable {
    private String deviceID;
    @NonNull
    private GuiPeer peer;


    public RecentPeer( String deviceID, String uniqueName, @Nullable Bitmap userImage) {
        this.deviceID = deviceID;
        this.peer= new GuiPeer(null,uniqueName,false,userImage);
    }

    public RecentPeer(String deviceID, String uniqueName, @Nullable byte[] userImage) {
        this.deviceID = deviceID;
        this.peer= new GuiPeer(null,uniqueName,false,userImage);
    }

    public RecentPeer(String deviceID, String uniqueName) {
        this.deviceID = deviceID;
        this.peer= new GuiPeer(null,uniqueName,false);
    }

    public RecentPeer(String deviceID) {
        this.deviceID = deviceID;
        this.peer= new GuiPeer(null,null,false);
    }

    public RecentPeer(@NonNull GuiPeer peer) {
        this.peer= peer;
    }

    public Bitmap getUserImage() {
        return peer.getUserImage();
    }

    public byte[] getUserImageData() {
        return peer.getUserImageData();
    }

    public void setUserImage(Bitmap userImage) {
        this.peer.setUserImage(userImage);
    }

    public void setDevice(BluetoothDevice device) {
        this.peer.setDevice(device);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof RecentPeer) {
            RecentPeer recentPeer = (RecentPeer) obj;
            if(deviceID!=null && recentPeer.deviceID!=null){
                return getDeviceID().equals(recentPeer.getDeviceID());
            }
        }

        return false;
    }

    @NonNull
    public String getDeviceID() {
        if(deviceID!=null){
            return deviceID;
        }else{
            return "";
        }
    }

    public String getName() {
        return peer.getName();
    }

    public String getUniqueName() {
        return peer.getUniqueName();
    }

    public boolean isAvailable() {
        return peer.getDevice()!=null;
    }

    @NonNull
    public Peer getPeer(){
        return peer;
    }

    //parcelable implementation
    public RecentPeer(Parcel in) {
        deviceID=in.readString();
        peer= (GuiPeer) Objects.requireNonNull(in.readParcelable(GuiPeer.class.getClassLoader()));
    }

    public static final Creator<RecentPeer> CREATOR = new Creator<RecentPeer>() {
        @Override
        public RecentPeer createFromParcel(Parcel in) {
            return new RecentPeer(in);
        }

        @Override
        public RecentPeer[] newArray(int size) {
            return new RecentPeer[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(deviceID);
        parcel.writeParcelable(peer,i);
    }
}
