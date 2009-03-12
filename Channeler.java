package maja_t372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static maja_t372.MessageConstants.*;
import static maja_t372.StateConstants.*;

public class Channeler extends AbstractRobot {

	public Channeler(RobotController _rc)
	{
		super(_rc);
	}

	public int run() throws GameActionException
	{
		isNearArchon = false;
		rc.setIndicatorString(0, rc.getRobotType().toString());
        while (true) {
            endRound();
            receiveMessages();
            senseNearbyRobots();
            protectNearestArchon();
        }
	}

    @Override
	protected void tryAttack() throws GameActionException
	{
		if (enemyRobotLocations != null && enemyRobotLocations.length > 0) {
			
			// chech if any target is in range
			for (MapLocation loc : enemyRobotLocations) {
                if ((loc != null) && (myLoc.distanceSquaredTo(loc) < 3)) {
                    rc.drain();
					actionQueued = true;
					//System.out.println("CHANNELER draining");///
					break;
                }
            };
			
			/*if (!actionQueued) {
				if (rc.getEnergonLevel() < 5)
					gotoLocation(new MapLocation(x_sum / num, y_sum / num));
				//no target in range - we have to go and get it!
				// kierujemy sie na srodek ciezkosci wrogow w zasiegu wzroku archona
				int x_sum = 0, y_sum = 0, num = 0;
				for (MapLocation loc : enemyRobotLocations) {
					if (loc != null) {
						x_sum += loc.getX();
						y_sum += loc.getY();
						num ++;
					};
				};
				if (num >0) {
					gotoLocation(new MapLocation(x_sum / num, y_sum / num));
				};
			};*/
		};
	}
}
