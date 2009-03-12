package maja_t372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;

import static maja_t372.MessageConstants.*;
import static maja_t372.StateConstants.*;
import static maja_t372.ErrorConstants.*;

public class Scout extends AbstractRobot {

	public Scout(RobotController _rc) {
		super(_rc);
	}

	public int run() throws GameActionException{
		state = STATE_IDLE;
		runLoop();
		return 0;
	}
}
