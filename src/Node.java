import java.util.Deque;
import java.util.LinkedList;
import java.util.Random;

import aiinterface.CommandCenter;
import enumerate.Action;
import simulator.Simulator;
import struct.CharacterData;
import struct.FrameData;
import struct.GameData;

/**
 * MCTS縺ｧ蛻ｩ逕ｨ縺吶ｋNode
 *
 * @author Taichi Miyazaki
 */
public class Node {

  /** UCT縺ｮ螳溯｡梧凾髢� */
  public static final int UCT_TIME = 165 * 100000;

  /** UCB1縺ｮ螳壽焚C縺ｮ蛟､ */
  public static final double UCB_C = 3;

  /** 謗｢邏｢縺吶ｋ譛ｨ縺ｮ豺ｱ縺� */
  public static final int UCT_TREE_DEPTH = 2;

  /** 繝弱�ｼ繝峨ｒ逕滓�舌☆繧矩明蛟､ */
  public static final int UCT_CREATE_NODE_THRESHOULD = 10;

  /** 繧ｷ繝溘Η繝ｬ繝ｼ繧ｷ繝ｧ繝ｳ繧定｡後≧譎る俣 */
  public static final int SIMULATION_TIME = 60;

  /** 荵ｱ謨ｰ繧貞茜逕ｨ縺吶ｋ縺ｨ縺阪↓菴ｿ縺� */
  private Random rnd;

  /** 隕ｪ繝弱�ｼ繝� */
  private Node parent;

  /** 蟄舌ヮ繝ｼ繝� */
  private Node[] children;

  /** 繝弱�ｼ繝峨�ｮ豺ｱ縺� */
  private int depth;

  /** 繝弱�ｼ繝峨′謗｢邏｢縺輔ｌ縺溷屓謨ｰ */
  private int games;

  /** UCB1蛟､ */
  private double ucb;

  /** 隧穂ｾ｡蛟､ */
  private double score;

  /** 驕ｸ謚槭〒縺阪ｋ閾ｪ蛻�縺ｮ蜈ｨAction */
  private LinkedList<Action> myActions;

  /** 驕ｸ謚槭〒縺阪ｋ逶ｸ謇九�ｮ蜈ｨAction */
  private LinkedList<Action> oppActions;

  /** 繧ｷ繝溘Η繝ｬ繝ｼ繧ｷ繝ｧ繝ｳ縺吶ｋ縺ｨ縺阪↓蛻ｩ逕ｨ縺吶ｋ */
  private Simulator simulator;

  /** 謗｢邏｢譎ゅ↓驕ｸ繧薙□閾ｪ蛻�縺ｮAction */
  private LinkedList<Action> selectedMyActions;

  /** 繧ｷ繝溘Η繝ｬ繝ｼ繧ｷ繝ｧ繝ｳ縺吶ｋ蜑阪�ｮ閾ｪ蛻�縺ｮHP */
  private int myOriginalHp;

  /** 繧ｷ繝溘Η繝ｬ繝ｼ繧ｷ繝ｧ繝ｳ縺吶ｋ蜑阪�ｮ逶ｸ謇九�ｮHP */
  private int oppOriginalHp;

  private FrameData frameData;
  private boolean playerNumber;
  private CommandCenter commandCenter;
  private GameData gameData;

  private boolean isCreateNode;

  Deque<Action> mAction;
  Deque<Action> oppAction;

  public Node(FrameData frameData, Node parent, LinkedList<Action> myActions,
      LinkedList<Action> oppActions, GameData gameData, boolean playerNumber,
      CommandCenter commandCenter, LinkedList<Action> selectedMyActions) {
    this(frameData, parent, myActions, oppActions, gameData, playerNumber, commandCenter);

    this.selectedMyActions = selectedMyActions;
  }

  public Node(FrameData frameData, Node parent, LinkedList<Action> myActions,
      LinkedList<Action> oppActions, GameData gameData, boolean playerNumber,
      CommandCenter commandCenter) {
    this.frameData = frameData;
    this.parent = parent;
    this.myActions = myActions;
    this.oppActions = oppActions;
    this.gameData = gameData;
    this.simulator = new Simulator(gameData);
    this.playerNumber = playerNumber;
    this.commandCenter = commandCenter;

    this.selectedMyActions = new LinkedList<Action>();

    this.rnd = new Random();
    this.mAction = new LinkedList<Action>();
    this.oppAction = new LinkedList<Action>();

    CharacterData myCharacter = frameData.getCharacter(playerNumber);
    CharacterData oppCharacter = frameData.getCharacter(!playerNumber);
    myOriginalHp = myCharacter.getHp();
    oppOriginalHp = oppCharacter.getHp();

    if (this.parent != null) {
      this.depth = this.parent.depth + 1;
    } else {
      this.depth = 0;
    }
  }

  /**
   * MCTS繧定｡後≧
   *
   * @return 譛�邨ら噪縺ｪ繝弱�ｼ繝峨�ｮ謗｢邏｢蝗樊焚縺悟､壹＞Action
   */
  public Action mcts() {
    // 譎る俣縺ｮ髯舌ｊ縲ゞCT繧堤ｹｰ繧願ｿ斐☆
    long start = System.nanoTime();
    for (; System.nanoTime() - start <= UCT_TIME;) {
      uct();
    }

    return getBestVisitAction();
  }

  /**
   * 繝励Ξ繧､繧｢繧ｦ繝�(繧ｷ繝溘Η繝ｬ繝ｼ繧ｷ繝ｧ繝ｳ)繧定｡後≧
   *
   * @return 繝励Ξ繧､繧｢繧ｦ繝育ｵ先棡縺ｮ隧穂ｾ｡蛟､
   */
  public double playout() {

    mAction.clear();
    oppAction.clear();

    for (int i = 0; i < selectedMyActions.size(); i++) {
      mAction.add(selectedMyActions.get(i));
    }

    for (int i = 0; i < 5 - selectedMyActions.size(); i++) {
      mAction.add(myActions.get(rnd.nextInt(myActions.size())));
    }

    for (int i = 0; i < 5; i++) {
      oppAction.add(oppActions.get(rnd.nextInt(oppActions.size())));
    }

    FrameData nFrameData =
        simulator.simulate(frameData, playerNumber, mAction, oppAction, SIMULATION_TIME); // 繧ｷ繝溘Η繝ｬ繝ｼ繧ｷ繝ｧ繝ｳ繧貞ｮ溯｡�

    return getScore(nFrameData);
  }

  /**
   * UCT繧定｡後≧ <br>
   *
   * @return 隧穂ｾ｡蛟､
   */
  public double uct() {

    Node selectedNode = null;
    double bestUcb;

    bestUcb = -99999;

    for (Node child : this.children) {
      if (child.games == 0) {
        child.ucb = 9999 + rnd.nextInt(50);
      } else {
        child.ucb = getUcb(child.score / child.games, games, child.games);
      }


      if (bestUcb < child.ucb) {
        selectedNode = child;
        bestUcb = child.ucb;
      }

    }

    double score = 0;
    if (selectedNode.games == 0) {
      score = selectedNode.playout();
    } else {
      if (selectedNode.children == null) {
        if (selectedNode.depth < UCT_TREE_DEPTH) {
          if (UCT_CREATE_NODE_THRESHOULD <= selectedNode.games) {
            selectedNode.createNode();
            selectedNode.isCreateNode = true;
            score = selectedNode.uct();
          } else {
            score = selectedNode.playout();
          }
        } else {
          score = selectedNode.playout();
        }
      } else {
        if (selectedNode.depth < UCT_TREE_DEPTH) {
          score = selectedNode.uct();
        } else {
          selectedNode.playout();
        }
      }

    }

    selectedNode.games++;
    selectedNode.score += score;

    if (depth == 0) {
      games++;
    }

    return score;
  }

  /**
   * 繝弱�ｼ繝峨ｒ逕滓�舌☆繧�
   */
  public void createNode() {

    this.children = new Node[myActions.size()];

    for (int i = 0; i < children.length; i++) {

      LinkedList<Action> my = new LinkedList<Action>();
      for (Action act : selectedMyActions) {
        my.add(act);
      }

      my.add(myActions.get(i));

      children[i] =
          new Node(frameData, this, myActions, oppActions, gameData, playerNumber, commandCenter,
              my);
    }
  }

  /**
   * 譛�螟夊ｨｪ蝠丞屓謨ｰ縺ｮ繝弱�ｼ繝峨�ｮAction繧定ｿ斐☆
   *
   * @return 譛�螟夊ｨｪ蝠丞屓謨ｰ縺ｮ繝弱�ｼ繝峨�ｮAction
   */
  public Action getBestVisitAction() {

    int selected = -1;
    double bestGames = -9999;

    for (int i = 0; i < children.length; i++) {

      if (SegmetonAI.DEBUG_MODE) {
        System.out.println("隧穂ｾ｡蛟､:" + children[i].score / children[i].games + ",隧ｦ陦悟屓謨ｰ:"
            + children[i].games + ",ucb:" + children[i].ucb + ",Action:" + myActions.get(i));
      }

      if (bestGames < children[i].games) {
        bestGames = children[i].games;
        selected = i;
      }
    }

    if (SegmetonAI.DEBUG_MODE) {
      System.out.println(myActions.get(selected) + ",蜈ｨ隧ｦ陦悟屓謨ｰ:" + games);
      System.out.println("");
    }

    return this.myActions.get(selected);
  }

  /**
   * 譛�螟壹せ繧ｳ繧｢縺ｮ繝弱�ｼ繝峨�ｮAction繧定ｿ斐☆
   *
   * @return 譛�螟壹せ繧ｳ繧｢縺ｮ繝弱�ｼ繝峨�ｮAction
   */
  public Action getBestScoreAction() {

    int selected = -1;
    double bestScore = -9999;

    for (int i = 0; i < children.length; i++) {

      System.out.println("隧穂ｾ｡蛟､:" + children[i].score / children[i].games + ",隧ｦ陦悟屓謨ｰ:"
          + children[i].games + ",ucb:" + children[i].ucb + ",Action:" + myActions.get(i));

      double meanScore = children[i].score / children[i].games;
      if (bestScore < meanScore) {
        bestScore = meanScore;
        selected = i;
      }
    }

    System.out.println(myActions.get(selected) + ",蜈ｨ隧ｦ陦悟屓謨ｰ:" + games);
    System.out.println("");

    return this.myActions.get(selected);
  }

  /**
   * 隧穂ｾ｡蛟､繧定ｿ斐☆
   *
   * @param fd 繝輔Ξ繝ｼ繝�繝�繝ｼ繧ｿ(縺薙ｌ縺ｫhp縺ｨ縺九�ｮ諠�蝣ｱ縺悟�･縺｣縺ｦ縺�繧�)
   * @return 隧穂ｾ｡蛟､
   */
  public int getScore(FrameData fd) {
    return (fd.getCharacter(playerNumber).getHp() - myOriginalHp) - (fd.getCharacter(!playerNumber).getHp() - oppOriginalHp);
  }

  /**
   * 隧穂ｾ｡蛟､縺ｨ蜈ｨ繝励Ξ繧､繧｢繧ｦ繝郁ｩｦ陦悟屓謨ｰ縺ｨ縺昴�ｮAction縺ｮ繝励Ξ繧､繧｢繧ｦ繝郁ｩｦ陦悟屓謨ｰ縺九ｉUCB1蛟､繧定ｿ斐☆
   *
   * @param score 隧穂ｾ｡蛟､
   * @param n 蜈ｨ繝励Ξ繧､繧｢繧ｦ繝郁ｩｦ陦悟屓謨ｰ
   * @param ni 縺昴�ｮAction縺ｮ繝励Ξ繧､繧｢繧ｦ繝郁ｩｦ陦悟屓謨ｰ
   * @return UCB1蛟､
   */
  public double getUcb(double score, int n, int ni) {
    return score + UCB_C * Math.sqrt((2 * Math.log(n)) / ni);
  }

  public void printNode(Node node) {
    System.out.println("蜈ｨ隧ｦ陦悟屓謨ｰ:" + node.games);
    for (int i = 0; i < node.children.length; i++) {
      System.out.println(i + ",蝗樊焚:" + node.children[i].games + ",豺ｱ縺�:" + node.children[i].depth
          + ",score:" + node.children[i].score / node.children[i].games + ",ucb:"
          + node.children[i].ucb);
    }
    System.out.println("");
    for (int i = 0; i < node.children.length; i++) {
      if (node.children[i].isCreateNode) {
        printNode(node.children[i]);
      }
    }
  }
}
