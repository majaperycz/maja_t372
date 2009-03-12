package maja_t372;

import battlecode.common.*;
import static maja_t372.StateConstants.*;

public class Cannon extends AbstractRobot{

	public Cannon(RobotController _rc)
	{
		super(_rc);
	}

    public int run() throws GameActionException{
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
    protected boolean isEnemyNearby() {
        return enemyRobotLocations != null;
    }

    @Override
    protected void tryAttack() throws GameActionException
	{
        if (enemyRobotLocations != null) {
            boolean attacked = false;
			boolean air = true;
            MapLocation best = null;
            int dist = Integer.MAX_VALUE;
            for (MapLocation loc : enemyRobotLocations) {
                if (loc == null) {
					air = false;
					continue;
				};
				if (rc.canAttackSquare(loc)) {
                    if (air) {
						if (!rc.canSenseSquare(loc) || (rc.senseAirRobotAtLocation(loc) != null)) {
							rc.attackAir(loc);
							//System.out.println("maja_t372: shooting in the air");
							attacked = true;
							break;
						};
					} else {
						if (!rc.canSenseSquare(loc) || (rc.senseGroundRobotAtLocation(loc) != null)) {
							rc.attackGround(loc);
							//System.out.println("maja_t372: shooting to the ground");
							attacked = true;
							break;
						};
                    };
                } else {
                    int dist1 = myLoc.distanceSquaredTo(loc);
                    if (dist1 > 1 && dist1 < dist) {
                        best = loc;
                        dist = dist1;
                    }
                }
            }
            if (!attacked && !rc.isMovementActive()) {
                if (best != null) {
                    gotoLocation(best);
                } else {
                    rc.setDirection(myDir.rotateRight());
                }
            }
        }
	}
}
