package duospire.networking.matchmaking;

import com.codedisaster.steamworks.SteamID;

import java.util.Comparator;

public class LobbyData {
    public SteamID id;
    public String name;
    public int ascension;
    public boolean isFriend = false;
    public boolean isPublic = true;
    public boolean sameMods = true;
    public boolean isValid = true;

    public void invalidate()
    {
        id = null;
        this.name = "";
        isPublic = false;
        isValid = false;
        ascension = 0;
    }

    public static class LobbyDataComparer implements Comparator<LobbyData>
    {
        @Override
        public int compare(LobbyData o1, LobbyData o2) {
            if (o1.isFriend ^ o2.isFriend)
            {
                return o1.isFriend ? -1 : 1;
            }
            else if (o1.isPublic ^ o2.isPublic)
            {
                return o1.isPublic ? -1 : 1;
            }
            else if (o1.sameMods ^ o2.sameMods) {
                return o1.sameMods ? -1 : 1;
            }
            else
            {
                return o1.name.compareTo(o2.name);
            }
        }

        @Override
        public boolean equals(Object obj) {
            return obj.getClass().equals(this.getClass());
        }
    }
}
