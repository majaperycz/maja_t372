package maja_t372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import static maja_t372.MessageConstants.*;
import static maja_t372.StateConstants.*;
import static maja_t372.ErrorConstants.*;

public class Messenger extends AbstractRobot {

	Message message;
	int targetArchon;

	public Messenger(RobotController _rc) {
		super(_rc);
	}

	public void setMessage(Message m) { message = m; }
	public void setTargetArchon(int tA) { targetArchon = tA; }
	public int getTargetArchon() { return targetArchon; }

	public int run() throws GameActionException{
		while(true) {
			sendMessages();			
			MapLocation[] alliedArchons = rc.senseAlliedArchons();
			if (alliedArchons.length < targetArchon)
				return ERROR_NO_SUCH_ARCHON;
			else if(alliedArchons[targetArchon].
					distanceSquaredTo(rc.getLocation()) <
					rc.getRobotType().broadcastRadius() - 1)
			{
				message.ints[INDEX_WHO] = rc.
					senseAirRobotAtLocation(alliedArchons[targetArchon]).
					getID();
				forwardMessage(message);
				while(!messageQueue.isEmpty())
					sendMessages();
				return ERROR_SUCCESS;
			}
			
			gotoLocation(alliedArchons[targetArchon]);
			rc.yield();
		}
	}
}
