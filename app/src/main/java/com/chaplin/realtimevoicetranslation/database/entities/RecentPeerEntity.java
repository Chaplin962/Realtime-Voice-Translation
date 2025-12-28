

package com.chaplin.realtimevoicetranslation.database.entities;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.chaplin.realtimevoicetranslation.voice_translation._conversation_mode.communication.recent_peer.RecentPeer;


@Entity
public class RecentPeerEntity {
    @androidx.annotation.NonNull
    @PrimaryKey
    public String deviceId="";
    public String uniqueName;
    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    public byte[] userImage;

    public RecentPeer getRecentPeer(){
        return new RecentPeer(deviceId,uniqueName,userImage);
    }
}
