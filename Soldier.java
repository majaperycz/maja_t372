package maja_t372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static maja_t372.MessageConstants.*;
import static maja_t372.StateConstants.*;

/**
 *
 * @author lukasz
 */
public class Soldier extends AbstractRobot{

	public Soldier(RobotController _rc)
	{
		super(_rc);
	}

	public int run() throws GameActionException
	{
		state = STATE_PROTECT_ARCHON;
        isNearArchon = false;
		runLoop();
		return 0;
	}

    @Override
    protected void tryAttack() throws GameActionException
	{
        RobotInfo enemy = getEnemy();
        if (enemy != null)
            attack(enemy);
	}

    @Override
    protected boolean isCloseToArchon() {
        return nearestArchon != null &&
                myLoc.distanceSquaredTo(nearestArchon) < 8;
    }
}
