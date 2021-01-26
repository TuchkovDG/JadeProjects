import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentSpeleologist extends Agent {

    private AID agentNavigator;
    private AID agentEnvironment;

    @Override
    protected void setup() {
    	System.out.println("Speleologist: Hello! Agent-Speleologist " + getAID().getName() + " is ready.");
        addBehaviour(new OtherAgentsFinder());
    }

    @Override
    protected void takeDown() {
    	ACLMessage prop = new ACLMessage(ACLMessage.CFP);
        prop.addReceiver(agentEnvironment);
        prop.setContent("terminate");
        prop.setConversationId(AgentEnvironment.Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
        prop.setReplyWith("prop" + System.currentTimeMillis());
        send(prop);
        ACLMessage prop2 = new ACLMessage(ACLMessage.INFORM);
        prop2.addReceiver(agentNavigator);
        prop2.setContent("terminate");
        prop2.setConversationId(AgentEnvironment.Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
        prop2.setReplyWith("prop2" + System.currentTimeMillis());
        send(prop2);
    	
        System.out.println("Speleologist: The speleologist agent " + getAID().getName() + " terminating.");
    }

    private class OtherAgentsFinder extends Behaviour {
        private boolean foundEnvironment = false;
        private boolean foundNavigator = false;

        @Override
        public void action() {
        	boolean foundNow = false;
        	if (!foundEnvironment) {
	            System.out.println("Speleologist: trying to find environment agent.");
	            DFAgentDescription template = new DFAgentDescription();
	            ServiceDescription sd = new ServiceDescription();
	            sd.setType(AgentEnvironment.Constants.WUMPUS_WORLD_TYPE);
	            template.addServices(sd);
	            try {
	                DFAgentDescription[] result = DFService.search(myAgent, template);
	                if (result != null && result.length > 0) {
	                	agentEnvironment = result[0].getName();
	                    System.out.println("Speleologist: successfully found environment agent.");
		                foundEnvironment = true;
		                foundNow = true;
	                } else {
	                    System.out.println("Speleologist: can not find environment agent.");
	                }
	            } catch (FIPAException e) {
	                e.printStackTrace();
	            }
        	}
        	if (!foundNavigator) {
	            System.out.println("Speleologist: trying to find navigator agent.");
	            DFAgentDescription template = new DFAgentDescription();
	            ServiceDescription sd = new ServiceDescription();
	            sd.setType(AgentEnvironment.Constants.NAVIGATOR_AGENT_TYPE);
	            template.addServices(sd);
	            try {
	                DFAgentDescription[] result = DFService.search(myAgent, template);
	                if (result != null && result.length > 0) {
	                	agentNavigator = result[0].getName();
	                    System.out.println("Speleologist: successfully found navigator agent.");
		                foundNavigator = true;
		                foundNow = true;
	                } else {
	                    System.out.println("Speleologist: can not find navigator agent.");
	                }
	            } catch (FIPAException e) {
	                e.printStackTrace();
	            }
        	}
        	if (foundNavigator && foundEnvironment && foundNow) {
        		myAgent.addBehaviour(new CaveExploration());
        	}
        }

        @Override
        public boolean done() {
            return foundNavigator && foundEnvironment;
        }
    }

    private class CaveExploration extends Behaviour {
        private int step = 1;
        private MessageTemplate mt;
        private ACLMessage reply;

        @Override
        public void action() {
            switch (step) {
                case 1:
                    ACLMessage req = new ACLMessage(ACLMessage.REQUEST);
                    req.addReceiver(agentEnvironment);
                    req.setContent(AgentEnvironment.Constants.GAME_INFORMATION);
                    req.setConversationId(AgentEnvironment.Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
                    req.setReplyWith("req" + System.currentTimeMillis());
                    myAgent.send(req);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId(AgentEnvironment.Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID),
                            MessageTemplate.MatchInReplyTo(req.getReplyWith()));
                    step++;
                    break;
                case 2:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            String rep = reply.getContent();
                            ACLMessage mes = new ACLMessage(ACLMessage.INFORM);
                            mes.addReceiver(agentNavigator);
                            
                            String infoSending = AgentEnvironment.Constants.INFO_SENDING1;
                            if (Math.random() > 0.5) {
                            	infoSending = AgentEnvironment.Constants.INFO_SENDING2;
                            }
                            String txt = infoSending + rep + " here.";
                            
                            mes.setContent(txt);
                            mes.setConversationId(AgentEnvironment.Constants.NAVIGATOR_DIGGER_CONVERSATION_ID);
                            mes.setReplyWith("mes" + System.currentTimeMillis());
                            System.out.println("Speleologist: " + txt);
                            myAgent.send(mes);
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(AgentEnvironment.Constants.NAVIGATOR_DIGGER_CONVERSATION_ID),
                                    MessageTemplate.MatchInReplyTo(mes.getReplyWith()));
                            step++;
                        }
                    } else
                        block();
                    break;
                case 3:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            String message = reply.getContent();
                            String action = parseNavigatorMessageProposal(message);
                            String content = "";

                            switch (action) {
                                case Commands.TURN_LEFT:
                                    content = AgentEnvironment.Constants.SPELEOLOGIST_TURN_LEFT;
                                    break;
                                case Commands.TURN_RIGHT:
                                    content = AgentEnvironment.Constants.SPELEOLOGIST_TURN_RIGHT;
                                    break;
                                case Commands.MOVE_FORWARD:
                                    content = AgentEnvironment.Constants.SPELEOLOGIST_MOVE_FORWARD;
                                    break;
                                case Commands.GRAB:
                                    content = AgentEnvironment.Constants.SPELEOLOGIST_GRAB;
                                    break;
                                case Commands.SHOOT:
                                    content = AgentEnvironment.Constants.SPELEOLOGIST_SHOOT;
                                    break;
                                case Commands.CLIMB:
                                    content = AgentEnvironment.Constants.SPELEOLOGIST_CLIMB;
                                    break;
                                default:
                                    System.out.println("Speleologist: There is no right action!");
                                    break;
                            }

                            ACLMessage prop = new ACLMessage(ACLMessage.CFP);
                            prop.addReceiver(agentEnvironment);
                            prop.setContent(content);
                            prop.setConversationId(AgentEnvironment.Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID);
                            prop.setReplyWith("prop" + System.currentTimeMillis());
                            myAgent.send(prop);
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId(AgentEnvironment.Constants.WUMPUS_WORLD_DIGGER_CONVERSATION_ID),
                                    MessageTemplate.MatchInReplyTo(prop.getReplyWith()));
                            step++;
                        }
                    } else {
                        block();
                    }
                    break;
                case 4:
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        String answer = reply.getContent();
                        switch (answer) {
                            case AgentEnvironment.Constants.FAIL_MESSAGE:
                                System.out.println("Speleologist: You failed!");
                                step++;
                                doDelete();
                                break;
                            case AgentEnvironment.Constants.OK_MESSAGE:
                                System.out.println("Speleologist: Wumpus world answers OK.");
                                step = 1;
                                break;
                            case AgentEnvironment.Constants.WIN_MESSAGE:
                                System.out.println("Speleologist: The speleologist survived and won!");
                                step++;
                                doDelete();
                                break;
                        }
                    } else {
                        block();
                    }
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 5;
        }

        private String parseNavigatorMessageProposal(String instruction) {
            for (Map.Entry<Integer, String> entry : Commands.WORDS.entrySet()) {
                String value = entry.getValue();
                Pattern pattern = Pattern.compile("\\b" + value + "\\b", Pattern.CASE_INSENSITIVE);
                Matcher matcher = pattern.matcher(instruction);
                if (matcher.find()) {
                    String res = matcher.group();
                    return res.length() > 0 ? res : "";
                }
            }
            return "";
        }

    }

    final static class Commands {
        static final String TURN_LEFT = "left";
        static final String TURN_RIGHT = "right";
        static final String MOVE_FORWARD = "forward";
        static final String GRAB = "grab";
        static final String SHOOT = "shoot";
        static final String CLIMB = "climb";
        static final Map<Integer, String> WORDS = new LinkedHashMap<Integer, String>() {{
            put(1, "left");
            put(2, "right");
            put(3, "forward");
            put(4, "grab");
            put(5, "shoot");
            put(6, "climb");
        }};
    }

}