package maja_t372;

import battlecode.common.*;
import static battlecode.common.GameConstants.*;
import static maja_t372.MessageConstants.*;
import static maja_t372.StateConstants.*;
import static maja_t372.ErrorConstants.*;
import java.util.*;

public abstract class AbstractRobot {

	protected final RobotController rc;
	protected final Rand rand;
	protected LinkedList<Message> messageQueue;
	protected boolean actionQueued;
	// actionQueued == true jesli zostala zakolejkowana akcja (lub jej "brak" -
	// z tym doNotMove() to i tak nie dziala)
	protected ArrayList<RobotInfo> nearbyPlayerRobots = null;
	protected ArrayList<RobotInfo> nearbyEnemyRobots = null;
	protected int nearbyEnemyRobotsAirCount = 0; //pierwsze nearbyEnemyRobotsAirCount z powyzszych to roboty lotne, pozostale sa naziemne
	protected MapLocation[] enemyRobotLocations = null;
	///protected enemyRobotsGroundCount = 0; //pierwsze enemyRobotsGroundCount z powyzszych to roboty naziemne, pozostale sa lotne
	///protected MapLocation[] enemyRobotLocationsGround = null;
	///protected MapLocation[] enemyRobotLocationsAir = null;
	// otrzymane od archona lokacje wrogich robotow
	protected MapLocation myLoc = null;
	protected Direction myDir = null;
	protected MapLocation nearestArchon = null;
	int bytecodeExplore;
	protected MapLocation lastEnemyLocation;
	protected int state;
	protected int previousState;
	protected int drainRounds; // ile rund jeszcz nalezy wykonywac drain()
	protected MapLocation gotoDestination = null;
	protected boolean isNearArchon = false;
	protected BugNavigator navigator;

	public void enemySpotted(MapLocation loc) {
		Message m = new Message();
		m.ints = new int[MESSAGE_MIN_INTS];
		m.locations = new MapLocation[1];
		m.ints[INDEX_CMD] = MSG_ENEMY_SPOTTED;
		m.locations[0] = loc;
		sendMessageToAll(m);
	}

	public void broadcastLocation(MapLocation loc) {
		Message m = new Message();
		m.ints = new int[MESSAGE_MIN_INTS + 1];
		m.locations = new MapLocation[1];
		m.ints[INDEX_CMD] = MSG_HERE_I_AM;
		m.ints[MESSAGE_MIN_INTS] = rc.getRobot().getID();
		m.locations[0] = loc;
		sendMessageToAll(m);
	}

	// TODO: abstract?
	protected void tryAttack() throws GameActionException {
		throw new GameActionException(GameActionExceptionType.INSUFFICIENT_ATTACK_COOLDOWN,
				"I don't know how to attack!");
	}

	protected MapLocation senseNearestArchonLocationFurtherThan(int d)
			throws GameActionException {
		MapLocation[] locs = rc.senseAlliedArchons();
		int minDist = Integer.MAX_VALUE;
        MapLocation nearest = null;
		for (MapLocation loc : locs) {
			int dist = loc.distanceSquaredTo(myLoc);
			if (dist < minDist && dist > d) {
				nearest = loc;
				minDist = dist;
			}
		}
        return nearest;
	}

    protected void senseNearestArchon() throws GameActionException {
		if (nearestArchon == null)
            nearestArchon = senseNearestArchonLocationFurtherThan(-1);
    }

    // Czy jestesmy tak blisko Archona, ze moze on nam przekazywac energon.
	protected boolean isArchonCloseEnough(MapLocation pos) {
		MapLocation pos2 = myLoc;
		return pos2.equals(pos) || pos2.isAdjacentTo(pos);
	}

	// TODO: abstract?
	// Czy jestesmy wystarczajaco blisko zeby chronic Archona, ale nie tak
	// blisko, zeby mogl on nam dawac energon.
	protected boolean isCloseToArchon() {
		return false;
	}

	protected boolean isEnemyNearby() throws GameActionException {
		return !nearbyEnemyRobots.isEmpty();
	}

	protected void gotoLocation(MapLocation loc) throws GameActionException {
		// TODO jak idzie to tez musi strzelac
		// to nie jest najlepsze miejsce, ale czemu nie?
		// (moze chodzic i strzelac naraz -
		// wcale wlasnie nie moze! nie mozna wykonywac dwoch akcji w tej samej
		// rundzie!)
		if (!rc.isAttackActive() && !rc.hasActionSet() && isEnemyNearby()) {
			RobotInfo enemy = getEnemy();
			if (enemy != null) {
				attack(enemy);
			}
		}
		if (rc.isMovementActive() || rc.hasActionSet()) {
			return;
		}

        if (myLoc.isAdjacentTo(loc) && (!rc.canSenseSquare(loc) || isEnemyAtLocation(loc))) {
            rc.setDirection(myLoc.directionTo(loc));
        } else {
            navigator.moveTo(loc);
        }
	}

	protected void protectNearestArchon() throws GameActionException {
        senseNearestArchon();
		if (!rc.isMovementActive()) {
			if (nearestArchon != null && !isArchonCloseEnough(nearestArchon)) {
				isNearArchon = false;
				gotoLocation(nearestArchon);
			} else {
				isNearArchon = true;
			}
		}
		if (!rc.hasActionSet() && !rc.isAttackActive() &&
				(isNearArchon || (isCloseToArchon() && isEnemyNearby()))) {
			tryAttack();
		}
	}

	// Returns true if successful.
	protected boolean attack(RobotInfo ri) throws GameActionException {
        if (rc.isAttackActive())
            return false;
		if (ri.type == RobotType.ARCHON || ri.type == RobotType.SCOUT) {
			if (rc.canAttackAir() && rc.canAttackSquare(ri.location)) {
				rc.attackAir(ri.location);
				return true;
			}
		} else {
			if (rc.canAttackGround() && rc.canAttackSquare(ri.location)) {
				rc.attackGround(ri.location);
				return true;
			}
		}
		return false;
	}

	protected boolean canAttack(RobotInfo ri) {
		if (ri.type == RobotType.ARCHON || ri.type == RobotType.SCOUT) {
			return rc.canAttackAir();
		} else {
			return rc.canAttackGround();
		}
	}

	protected RobotInfo getEnemy() throws GameActionException {
		RobotInfo best = null;
		int dist = Integer.MAX_VALUE;
		for (RobotInfo enemy : nearbyEnemyRobots) {
			if (canAttack(enemy)) {
				int dist2 = enemy.location.distanceSquaredTo(myLoc);
				if (dist2 < dist) {
					dist = dist2;
					best = enemy;
				}
			}
		}
		return best;
	}

    protected boolean isEnemyAtLocation(MapLocation loc) throws GameActionException {
        RobotInfo ri;
        Robot r = rc.senseGroundRobotAtLocation(loc);
        if (r != null) {
            ri = rc.senseRobotInfo(r);
            if (ri.team != rc.getTeam()) {
                return true;
            }
        }
        r = rc.senseAirRobotAtLocation(loc);
        if (r != null) {
            ri = rc.senseRobotInfo(r);
            if (ri.team != rc.getTeam()) {
                return true;
            }
        }
        return false;
    }

	protected void offensive() throws GameActionException {
		if (isEnemyNearby()) {
			RobotInfo enemy = getEnemy();
			if (enemy != null && !attack(enemy)) {
				gotoLocation(enemy.location);
			}
		} else if (lastEnemyLocation != null) {
			gotoLocation(lastEnemyLocation);
		} else if (!rc.isMovementActive()) {
			doExplore();
		}
	}

	protected void receiveMessages() throws GameActionException {
		Message msg = null;
		for (int ii = 0; ii < 6 && (msg = getMessage()) != null; ++ii) {
			switch (msg.ints[INDEX_CMD]) {
				case MSG_START_OFFENSIVE:
					state = STATE_OFFENSIVE;
					break;
				case MSG_PROTECT_ARCHON:
					state = STATE_PROTECT_ARCHON;
					isNearArchon = false;
					break;
				case MSG_GO_ATTACK:
					if (state != STATE_OFFENSIVE) {
						state = STATE_GO_ATTACK;
						gotoDestination = msg.locations[0];
					} else {
						lastEnemyLocation = msg.locations[0];
					}
					break;
				case MSG_ENEMY_SPOTTED:
					lastEnemyLocation = msg.locations[0];
					break;
				case MSG_DRAIN:
					drainRounds = 3;
					break;
				case MSG_HELP: // fall through
				case MSG_LETTER:
					if (!rc.getRobotType().equals(RobotType.SCOUT)) {
						break;
					}
					rc.setIndicatorString(0, "SCOUT: " + (msg.ints[INDEX_CMD] == MSG_HELP ? "HELP" : "Messenger"));
					Messenger messenger = new Messenger(rc);
					messenger.setMessage(msg);
					messenger.setTargetArchon(msg.ints[3]);
					if (messenger.run() == ERROR_SUCCESS) {
						rc.setIndicatorString(2, "Messenger mission succeded");
					} else {
						rc.setIndicatorString(2, "Messenger mission failed");
					}
					break;
				case MSG_ENEMY_LOCATIONS:
					enemyRobotLocations = msg.locations;
					break;
				default:
					break;
			}
		} // end for
	}

	protected void runLoop() throws GameActionException {
		while (true) {
			endRound();
			senseNearbyRobots();
			receiveMessages();
			switch (state) {
				case STATE_PROTECT_ARCHON:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Protect Archon");
					protectNearestArchon();
					break;
				case STATE_OFFENSIVE:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Offensive");
					offensive();
					break;
				case STATE_GO_ATTACK:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Moving");
					rc.setIndicatorString(1, myLoc.toString() + " -> " + gotoDestination.toString());
					if (rc.isMovementActive()) {
						break;
					}
					if (myLoc.distanceSquaredTo(gotoDestination) <= 1) {
						state = STATE_OFFENSIVE;
					} else {
						gotoLocation(gotoDestination);
					}
					break;
				case STATE_IDLE:
					rc.setIndicatorString(0, rc.getRobotType().toString() + ": Idle");
					break;
				default:
					break;
			}
		}
	}

	protected void endRound() throws GameActionException {
		// clear caches first
		nearbyEnemyRobots = null;
		nearbyEnemyRobotsAirCount = 0;
		nearbyPlayerRobots = null;
		nearestArchon = null;
		enemyRobotLocations = null;
		actionQueued = false;
		if (drainRounds > 0) {
			--drainRounds;
		}
		rc.yield();
        myLoc = rc.getLocation();
		myDir = rc.getDirection();
        if (lastEnemyLocation != null &&
                myLoc.distanceSquaredTo(lastEnemyLocation) < 8 &&
                rc.canSenseSquare(lastEnemyLocation) &&
                !isEnemyAtLocation(lastEnemyLocation)) {
            lastEnemyLocation = null;
        }
	}

	protected void setState(int newState) {
		if (state != newState) {
			previousState = state;
			state = newState;
		}
	}

	/*
	 * CLEAN METHODS
	 */
	public AbstractRobot(RobotController _rc) {
		rc = _rc;
		messageQueue = new LinkedList<Message>();
		rand = new Rand();
		navigator = new BugNavigator(rc);
	}

	abstract public int run() throws GameActionException;

	final int messageHash(Message m) {
		// w sumie trudno nam zaszkodzic przez zmienianie intow, wiec
		// olejmy to, bo to duzo bytecodeow zzera
		/*for (int ii = INDEX_HASH + 1; ii < m.ints.length; ++ii) {
			hash += m.ints[ii];
			hash ^= (hash << 16) ^ (m.ints[ii] << 11);
			hash += hash >> 11;
		}*/
		// ale chcemy aby hash byl zalezny od rundy
		int hash = MESSAGE_HASH_START + m.ints[INDEX_ROUND] ^ (MESSAGE_HASH_START << 3);
		// chcemy sprawdzic lokacje
		if (m.locations != null) {
			for (int ii = 0; ii < m.locations.length; ++ii) {
				if (m.locations[ii] != null) {
					hash += m.locations[ii].getX() ^ (hash << 3);
					hash += m.locations[ii].getY() ^ (hash << 3);
				};
			}
		}
		hash += MESSAGE_HASH_START ^ (hash << 3);
		return hash;
	}
	
	final Message getMessage() {
		Message m = null;
		while ((m = rc.getNextMessage()) != null) {
			if (m.ints != null && 
				m.ints.length >= MESSAGE_MIN_INTS &&
				m.ints[INDEX_TAG] == MESSAGE_TAG &&
				(m.ints[INDEX_WHO] == rc.getRobot().getID() ||
				m.ints[INDEX_WHO] == ID_BROADCAST) &&
				(m.ints[INDEX_ROUND] >> 3) + 30 >= Clock.getRoundNum() &&
				messageHash(m) == m.ints[INDEX_HASH]) {
				return m;
			} 
		}
		return null;
	}

	final void enqueueMessage(Message m) {
		messageQueue.addLast(m);
	}

	final void forwardMessage(Message m) {
		m.ints[INDEX_TAG] = MESSAGE_TAG;
		m.ints[INDEX_ROUND] = (Clock.getRoundNum() << 3) + 1;
		m.ints[INDEX_HASH] = messageHash(m);
		enqueueMessage(m);
	}

	final void sendMessageTo(int who, Message m) {
		m.ints[INDEX_TAG] = MESSAGE_TAG;
		m.ints[INDEX_ROUND] = (Clock.getRoundNum() << 3) + 1;
		m.ints[INDEX_WHO] = who;
		m.ints[INDEX_HASH] = messageHash(m);
		enqueueMessage(m);
	}

	final void sendMessageToAll(Message m) {
		m.ints[INDEX_TAG] = MESSAGE_TAG;
		m.ints[INDEX_ROUND] = (Clock.getRoundNum() << 3) + 1;
		m.ints[INDEX_WHO] = ID_BROADCAST;
		m.ints[INDEX_HASH] = messageHash(m);
		enqueueMessage(m);
	}

	final void sendMessages() throws GameActionException {
		if (!rc.hasBroadcastMessage() && !messageQueue.isEmpty()) {
			rc.broadcast(messageQueue.poll());
		}

	}

	// Obsluga otoczenia
	final void senseNearbyRobots() throws GameActionException {
//        int a = Clock.getBytecodeNum();
		nearbyPlayerRobots = new ArrayList<RobotInfo>();
		nearbyEnemyRobots =	new ArrayList<RobotInfo>();
		nearbyEnemyRobotsAirCount = 0;
		RobotInfo ri;

		Robot[] robots = rc.senseNearbyAirRobots();
		for (Robot robot : robots) {
			ri = rc.senseRobotInfo(robot);
			if (ri.team == rc.getTeam()) {
				nearbyPlayerRobots.add(ri);
			} else {
				nearbyEnemyRobots.add(ri);
				nearbyEnemyRobotsAirCount++;
			}

		}
		robots = rc.senseNearbyGroundRobots();
		for (Robot robot : robots) {
			ri = rc.senseRobotInfo(robot);
			if (ri.team == rc.getTeam()) {
				nearbyPlayerRobots.add(ri);
			} else {
				nearbyEnemyRobots.add(ri);
			}

		}
//        System.out.print("senseNearbyRobots bytecodes: ");
//        System.out.println(Clock.getBytecodeNum() - a);
	}

	/**
	 * Oblicza kierunek ucieczki jako kierunek od srodka ciezkosci miedzy
	 * robotami przeciwnika do wlasnej pozycji.
	 * Cannony maja 2 razy wieksza wage niz pozostale roboty.
	 * Workery nie sa liczone. 
	 * Zwraca NONE jesli nie ma przeciwnikow w poblizu.
	 */
	Direction calculateEscapeDirection() {
		int x_sum = 0, y_sum = 0, num = 0;
		for (RobotInfo r : nearbyEnemyRobots) {
			if (r.type == RobotType.CANNON) {
				x_sum += 2 * r.location.getX();
				y_sum +=
						2 * r.location.getY();
				num +=
						2;
			} else if (r.type != RobotType.WORKER) {
				x_sum += r.location.getX();
				y_sum +=
						r.location.getY();
				num++;

			}


		}
		if (num == 0) {
			return Direction.NONE;
		}

		MapLocation massCentre = new MapLocation(x_sum / num, y_sum / num);
		return massCentre.directionTo(myLoc);
	}

// obsluga celow
	void doExplore() throws GameActionException {
		//	if (Clock.getBytecodeNum() > bytecodeExplore) return;
		//	int a = Clock.getBytecodeNum();
		if (rc.isMovementActive()) {
			return;
		}
		rc.setIndicatorString(0, "Exploring");
		if (!rc.canMove(myDir) || Clock.getRoundNum() % 8 == 0) {
			if (rand.getBoolean()) {
				rc.setDirection(myDir.rotateLeft());
			} else {
				rc.setDirection(myDir.rotateRight());
			}
		} else {
			rc.moveForward();
		}
		actionQueued = true;
	//	if (Clock.getBytecodeNum() > a &&
	//			bytecodeExplore < a) bytecodeExplore = a;
	}
}
