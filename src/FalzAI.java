import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Random;
import aiinterface.AIInterface;
import aiinterface.CommandCenter; 
import enumerate.Action;
import enumerate.State;
import simulator.Simulator;
import struct.FrameData;
import struct.GameData;
import struct.Key;
import struct.CharacterData;
import struct.MotionData;

public class FalzAI implements AIInterface {

	private Action[] actionAir;
	private Action[] actionGround;
	private Action[] actionAirDef;
	private Action[] actionAirOff;
	private Action[] actionGroundDef;
	private Action[] actionGroundOff;
	private Action spSkill;
	private ArrayList<MotionData> playerMotion;
	private ArrayList<MotionData> opponentMotion;
	private boolean playerNumber;
	private CharacterData player;
	private CharacterData opponent;
	private CommandCenter commandCenter;
	private FrameData frameData;
	private FrameData simulatorAheadFrameData;
	private GameData gameData;
	private Key key;
	private LinkedList<Action> playerActions;
	private LinkedList<Action> opponentActions;
	private Node rootNode;
	private Random rng;
	private Simulator simulator;
	
	public static final boolean DEBUG_MODE = true;
	private static final int FRAME_AHEAD = 14;
	
	@Override
	public void close() {
		// TODO Auto-generated method stub

	}

	@Override
	public void getInformation(FrameData fd) {
		frameData = fd;
		commandCenter.setFrameData(frameData, playerNumber);
		player = frameData.getCharacter(playerNumber);
		opponent = frameData.getCharacter(!playerNumber);

	}

	@Override
	public int initialize(GameData gd, boolean playerNum) {
		playerNumber = playerNum;
		gameData = gd;
		key = new Key();
		commandCenter = new CommandCenter();
		frameData = new FrameData();
		playerActions = new LinkedList<Action>();
	    opponentActions = new LinkedList<Action>();
		
		simulator = gameData.getSimulator();

		actionAir = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DA, Action.AIR_DB,
				Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FA, Action.AIR_D_DF_FB,
				Action.AIR_F_D_DFA, Action.AIR_F_D_DFB, Action.AIR_D_DB_BA, Action.AIR_D_DB_BB };
		
		actionGround = new Action[] { Action.STAND_D_DB_BA, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB };
		
		spSkill = Action.STAND_D_DF_FC;
		
		actionAirOff = new Action[] { Action.AIR_GUARD, Action.AIR_A, Action.AIR_B, Action.AIR_DB,
				Action.AIR_FA, Action.AIR_FB, Action.AIR_UA, Action.AIR_UB, Action.AIR_D_DF_FB,
				Action.AIR_F_D_DFB, Action.AIR_D_DB_BB };
		
		actionGroundOff = new Action[] { 
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.STAND_D_DF_FB, Action.STAND_D_DB_BA,
				Action.STAND_F_D_DFA, Action.STAND_F_D_DFB, Action.STAND_D_DB_BB, Action.BACK_STEP, Action.FORWARD_WALK, Action.DASH,
				Action.JUMP, Action.FOR_JUMP, Action.BACK_JUMP, Action.STAND_GUARD, Action.CROUCH_GUARD };
		
		actionAirDef = new Action[] { Action.AIR_GUARD, Action.AIR_D_DF_FA, Action.AIR_DA, Action.AIR_FA, 
				Action.AIR_FB, Action.AIR_UA, Action.AIR_D_DF_FA, Action.AIR_D_DF_FB, Action.AIR_F_D_DFB};
		
		actionGroundDef = new Action[] { Action.STAND_GUARD, Action.CROUCH_GUARD, Action.THROW_A,
				Action.THROW_B, Action.STAND_A, Action.STAND_B, Action.CROUCH_A, Action.CROUCH_B, Action.STAND_FA,
				Action.STAND_FB, Action.CROUCH_FA, Action.CROUCH_FB, Action.STAND_D_DF_FA, Action.STAND_D_DF_FB, Action.FORWARD_WALK,
				Action.JUMP, Action.BACK_JUMP, Action.BACK_STEP};

		playerMotion = gameData.getMotionData(playerNumber);
		opponentMotion = gameData.getMotionData(!playerNumber);
		
		return 0;
	}

	@Override
	public Key input() {
		return key;
	}

	@Override
	public void processing() {
		if(canProcessing()) {
			if (commandCenter.getSkillFlag()) {
				key = commandCenter.getSkillKey();
			} else {
				key.empty();
				commandCenter.skillCancel();
				
				if(isDown()) {
					DebugLog(Action.CROUCH_GUARD.name());
					commandCenter.commandCall(Action.CROUCH_GUARD.name());
				}else {
					mctsPrepare();
					rootNode = new Node(simulatorAheadFrameData, null, playerActions, opponentActions, gameData, playerNumber,
							commandCenter);
					rootNode.createNode();

					Action bestAction = rootNode.mcts();
					if (FalzAI.DEBUG_MODE) {
						rootNode.printNode(rootNode);
					}

					commandCenter.commandCall(bestAction.name());
				}

				
			}
		}
	}

	@Override
	public void roundEnd(int p1Hp, int p2Hp, int frames) {
		key.empty();
		commandCenter.skillCancel();
	}
	
	public boolean canProcessing() {
		return !frameData.getEmptyFlag() && frameData.getRemainingFramesNumber() > 0;
	}
	
	public void mctsPrepare() {
		simulatorAheadFrameData = simulator.simulate(frameData, playerNumber, null, null, FRAME_AHEAD);

		player = simulatorAheadFrameData.getCharacter(playerNumber);
		opponent = simulatorAheadFrameData.getCharacter(!playerNumber);

		setPlayerAction();
		setOpponentAction();
	}

	public void setPlayerAction() {
		playerActions.clear();

		int energy = player.getEnergy();
		int playerHp = player.getHp();
		int opponentHp = opponent.getHp();
		int hpDiff = playerHp - opponentHp; 
		
		if (FalzAI.DEBUG_MODE) {
	        System.out.println("Player HP:" + playerHp + " Opponent HP: " + opponentHp + " Diff: " + Math.abs(hpDiff));
	    }
		
		if(Math.abs(hpDiff) > 30 && opponentHp > playerHp){
			if(opponent.getState() == State.AIR) {
				airDefensive(energy);
			}else {
				spSkill(energy);
				groundDefensive(energy);
			}
		}else {
			if(opponent.getState() == State.AIR) {
				airOffensive(energy);
			}else {
				spSkill(energy);
				groundOffensive(energy);
			}
		}
	}

	public void setOpponentAction() {
		opponentActions.clear();

		int energy = opponent.getEnergy();

		if (opponent.getState() == State.AIR) {
			for (int i = 0; i < actionAir.length; i++) {
				if (Math.abs(opponentMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					opponentActions.add(actionAir[i]);
				}
			}
		} else {
			if (Math.abs(opponentMotion.get(Action.valueOf(spSkill.name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				opponentActions.add(spSkill);
			}

			for (int i = 0; i < actionGround.length; i++) {
				if (Math.abs(opponentMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
						.getAttackStartAddEnergy()) <= energy) {
					opponentActions.add(actionGround[i]);
				}
			}
		}
	}
	
	private void spSkill(int energy) {
		if (Math.abs(
				playerMotion.get(Action.valueOf(spSkill.name()).ordinal()).getAttackStartAddEnergy()) <= energy) {
			playerActions.add(spSkill);
		}
	}
	
	private void airDefensive(int energy) {
		if (FalzAI.DEBUG_MODE) {
	        System.out.println("Go Defensive");
	    }
		for (int i = 0; i < actionAirDef.length; i++) {
			if (Math.abs(playerMotion.get(Action.valueOf(actionAirDef[i].name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				playerActions.add(actionAirDef[i]);
			}
		}
	}
	
	private void airOffensive(int energy) {
		if (FalzAI.DEBUG_MODE) {
	        System.out.println("Go Offensive");
	    }
		
		for (int i = 0; i < actionAirOff.length; i++) {
			if (Math.abs(playerMotion.get(Action.valueOf(actionAirOff[i].name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				playerActions.add(actionAirOff[i]);
			}
		}
	}
	
	private void groundDefensive(int energy) {
		if (FalzAI.DEBUG_MODE) {
	        System.out.println("Go Defensive");
	    }
		
		for (int i = 0; i < actionGroundDef.length; i++) {
			if (Math.abs(playerMotion.get(Action.valueOf(actionGroundDef[i].name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				playerActions.add(actionGroundDef[i]);
			}
		}
	}
	
	private void groundOffensive(int energy) {
		if (FalzAI.DEBUG_MODE) {
	        System.out.println("Go Offensive");
	    }
		
		for (int i = 0; i < actionGroundOff.length; i++) {
			if (Math.abs(playerMotion.get(Action.valueOf(actionGroundOff[i].name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				playerActions.add(actionGroundOff[i]);
			}
		}
	}
	
	private void airMove(int energy) {
		for (int i = 0; i < actionAir.length; i++) {
			if (Math.abs(playerMotion.get(Action.valueOf(actionAir[i].name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				playerActions.add(actionAir[i]);
			}
		}
	}
	
	private void groundMove(int energy) {
		for (int i = 0; i < actionGround.length; i++) {
			if (Math.abs(playerMotion.get(Action.valueOf(actionGround[i].name()).ordinal())
					.getAttackStartAddEnergy()) <= energy) {
				playerActions.add(actionGround[i]);
			}
		}
	}
	
	private Boolean isDown(){
		Action playerAction = player.getAction();
		State playerState = player.getState();
		
		if( (playerAction == Action.DOWN || playerAction == Action.RISE || playerAction == Action.CHANGE_DOWN) && playerState != State.AIR) {
			DebugLog("Down");
			return true;
		}
		
		return false;
	}
	
	private void DebugLog(String text) {
		if (FalzAI.DEBUG_MODE) {
	        System.out.println(text);
	    }
	}

}
