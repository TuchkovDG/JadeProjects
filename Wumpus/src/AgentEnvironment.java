import com.sun.prism.image.Coords;
import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;

public class AgentEnvironment extends Agent {

    private WumpusCave cave;
    private Position agentPosition;
    private boolean isWumpusAlive = true;
    private boolean isGoldGrabbed = false;
    private boolean hasArrow = true;

    public AgentEnvironment() {
        this.cave = new WumpusCave(4, 4, Constants.INITIAL_WUMPUS_CAVE);
        agentPosition = cave.getStart();
    }

    @Override
    protected void setup() {
    	System.out.println("Hello! Agent-Environment " + getAID().getName() + " is ready.");
        System.out.println("Environment: initial world state:");
        System.out.println(cave);

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(Constants.WUMPUS_WORLD_TYPE);
        sd.setName(Constants.WUMPUS_SERVICE_DESCRIPTION);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        addBehaviour(new InformationSender());
        addBehaviour(new ActionPerformer());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Environment: Agent-Environment " + getAID().getName() + " terminating.");
    }

    private class InformationSender extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String message = msg.getContent();
                if (message.equals(Constants.GAME_INFORMATION)) {
                    ACLMessage reply = msg.createReply();
                    reply.setPerformative(ACLMessage.INFORM);
                    WumpusPercept current = getPerceptSeenBy();
                    String g = current.toString();
                    reply.setContent(g);
                    myAgent.send(reply);
                }
            } else {
                block();
            }
        }
    }

    private class ActionPerformer extends CyclicBehaviour {

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String message = msg.getContent();

                
                if (message.contains("terminate")) {
                	myAgent.doDelete();
                	return;
                }
                
                boolean sendTerminateMessage = false;
                boolean sendWinMessage = false;

                switch (message){
                    case Constants.SPELEOLOGIST_TURN_LEFT: turnLeft(); break;
                    case Constants.SPELEOLOGIST_TURN_RIGHT: turnRight(); break;
                    case Constants.SPELEOLOGIST_MOVE_FORWARD: sendTerminateMessage = moveForward(); break;
                    case Constants.SPELEOLOGIST_GRAB: grab();  break;
                    case Constants.SPELEOLOGIST_SHOOT: shoot(); break;
                    case Constants.SPELEOLOGIST_CLIMB: if (climb()) sendWinMessage = true; else sendTerminateMessage = true; break;
                    default: System.out.println("Environment: Wrong action!"); break;
                }

                System.out.println("Environment: make action " + message) ;
                System.out.println("Environment: state after the action:");
                System.out.println(cave.toStringWithStart(agentPosition, isGoldGrabbed));

                sendTerminateMessage |= sendWinMessage;
                ACLMessage reply = msg.createReply();
                reply.setPerformative(ACLMessage.ACCEPT_PROPOSAL);
                if (!sendTerminateMessage)
                    reply.setContent(Constants.OK_MESSAGE);
                else if (sendWinMessage){
                    reply.setContent(Constants.WIN_MESSAGE);
                }
                else {
                    reply.setContent(Constants.FAIL_MESSAGE);
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }

    private void turnLeft() {
    	agentPosition = cave.turnLeft(agentPosition);
    }

    private void turnRight() {
    	agentPosition = cave.turnRight(agentPosition);
    }

    private boolean moveForward() {
    	agentPosition = cave.moveForward(agentPosition);
        return (isWumpusAlive && cave.getWumpus().equals(agentPosition.getRoom())) || cave.isPit(agentPosition.getRoom());
    }

    private void grab() {
        if (cave.getGold().equals(agentPosition.getRoom()))
            isGoldGrabbed = true;
    }

    private void shoot() {
        if (hasArrow && isAgentFacingWumpus(agentPosition))
            isWumpusAlive = false;
    }

    private boolean climb() {
        return agentPosition.getRoom().equals(new Room(1, 1)) && isGoldGrabbed;
    }

    private boolean isAgentFacingWumpus(Position pos) {
        Room wumpus = cave.getWumpus();
        switch (pos.getOrientation()) {
            case FACING_NORTH:
                return pos.getX() == wumpus.getX() && pos.getY() < wumpus.getY();
            case FACING_SOUTH:
                return pos.getX() == wumpus.getX() && pos.getY() > wumpus.getY();
            case FACING_EAST:
                return pos.getY() == wumpus.getY() && pos.getX() < wumpus.getX();
            case FACING_WEST:
                return pos.getY() == wumpus.getY() && pos.getX() > wumpus.getX();
        }
        return false;
    }

    public WumpusPercept getPerceptSeenBy() {
        WumpusPercept result = new WumpusPercept();
        Position pos = agentPosition;
        List<Room> adjacentRooms = Arrays.asList(
                new Room(pos.getX()-1, pos.getY()), new Room(pos.getX()+1, pos.getY()),
                new Room(pos.getX(), pos.getY()-1), new Room(pos.getX(), pos.getY()+1)
        );
        List<Room> adjacentRoomsFull = new LinkedList<>();
        for (Room r : adjacentRooms) {
        adjacentRoomsFull.addAll(Arrays.asList(new Room(r.getX()-1, r.getY()), new Room(r.getX()+1, r.getY()),
                new Room(r.getX(), r.getY()-1), new Room(r.getX(), r.getY()+1)));
        }
        adjacentRoomsFull.addAll(adjacentRooms);
        for (Room r : adjacentRoomsFull) {
            if (r.equals(cave.getWumpus()))
                result.setStench();
            if (cave.isPit(r))
                result.setBreeze();
        }
        if (pos.getRoom().equals(cave.getGold()))
            result.setGlitter();
        if (!isWumpusAlive)
            result.setScream();
        return result;
    }

    final static class Constants {
        static final String WUMPUS_WORLD_TYPE = "wumpus_world";
        static final String WUMPUS_SERVICE_DESCRIPTION = "wumpus_world";
        static final String NAVIGATOR_SERVICE_DESCRIPTION = "navigator";
        static final String NAVIGATOR_AGENT_TYPE = "navigator_agent";
        static final String WUMPUS_WORLD_DIGGER_CONVERSATION_ID = "digger_conv_id";
        static final String NAVIGATOR_DIGGER_CONVERSATION_ID = "navigator_conv_id";
        static final String INITIAL_WUMPUS_CAVE = ". . . P W G . . . . . . S . P . ";
        static final String OK_MESSAGE = "OK";
        static final String FAIL_MESSAGE = "FAIL";
        static final String WIN_MESSAGE = "WIN";
        static final String SPELEOLOGIST_TURN_LEFT = "SPELEOLOGIST_TURN_LEFT";
        static final String SPELEOLOGIST_TURN_RIGHT = "SPELEOLOGIST_TURN_RIGHT";
        static final String SPELEOLOGIST_MOVE_FORWARD = "SPELEOLOGIST_MOVE_FORWARD";
        static final String SPELEOLOGIST_GRAB = "SPELEOLOGIST_GRAB";
        static final String SPELEOLOGIST_SHOOT = "SPELEOLOGIST_SHOOT";
        static final String SPELEOLOGIST_CLIMB = "SPELEOLOGIST_CLIMB";
        static final String GAME_INFORMATION = "INFORMATION";
        
        static final String INFO_SENDING1 = "I feel ";
        static final String INFO_SENDING2 = "There is ";
        
        static final String ACTION_PROPOSAL1 = "You should ";
        static final String ACTION_PROPOSAL2 = "I think it is a good option to ";
        static final String ACTION_PROPOSAL3 = "Maybe you can ";

        static final String MESSAGE_LEFT = "turn left.";
        static final String MESSAGE_RIGHT = "turn right.";
        static final String MESSAGE_FORWARD = "move forward.";
        static final String MESSAGE_GRAB = "grab the gold.";
        static final String MESSAGE_SHOOT = "shoot.";
        static final String MESSAGE_CLIMB = "climb the ladder.";
    }

}