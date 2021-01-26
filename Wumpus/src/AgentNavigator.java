import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AgentNavigator extends Agent {

    @Override
    protected void setup() {
    	System.out.println("Navigator: Hello! Agent-Navigator " + getAID().getName() + " is ready.");

        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType(AgentEnvironment.Constants.NAVIGATOR_AGENT_TYPE);
        sd.setName(AgentEnvironment.Constants.NAVIGATOR_SERVICE_DESCRIPTION);
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        addBehaviour(new ActionProposal());
    }

    @Override
    protected void takeDown() {
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Navigator: The navigator agent " + getAID().getName() + " terminating.");
    }

    private class ActionProposal extends CyclicBehaviour {

        int time = 0;

        @Override
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
            	if (msg.getContent().contains("terminate")) {
                	myAgent.doDelete();
                	return;
                }
                ACLMessage reply = msg.createReply();
                System.out.println("Navigator: recieve information " + parseSpeleologistInformation(msg.getContent()));
                reply.setPerformative(ACLMessage.PROPOSE);
                String advice = getAdvice(msg.getContent());
                reply.setContent(advice);
                System.out.println("Navigator: " + advice);
                myAgent.send(reply);
            } else {
                block();
            }
        }

        private WumpusPercept parseSpeleologistInformation(String info) {
        	WumpusPercept res = new WumpusPercept();
        	if (info.contains("Stench")) res.setStench();
        	if (info.contains("Breeze")) res.setBreeze();
        	if (info.contains("Glitter")) res.setGlitter();
        	if (info.contains("Bump")) res.setBump();
        	if (info.contains("Scream")) res.setScream();
        	return res;
        }

        private String getAdvice(String info){
        	WumpusPercept percept = parseSpeleologistInformation(info);
        	String advicedAction = "";
            switch (time) {
                case 0: advicedAction = AgentEnvironment.Constants.MESSAGE_FORWARD; break;
                case 1: advicedAction = AgentEnvironment.Constants.MESSAGE_RIGHT; break;
                case 2: advicedAction = AgentEnvironment.Constants.MESSAGE_FORWARD; break;
                case 3: advicedAction = AgentEnvironment.Constants.MESSAGE_LEFT; break;
                case 4: advicedAction = AgentEnvironment.Constants.MESSAGE_FORWARD; break;
                case 5: advicedAction = AgentEnvironment.Constants.MESSAGE_GRAB; break;
                case 6: advicedAction = AgentEnvironment.Constants.MESSAGE_LEFT; break;
                case 7: advicedAction = AgentEnvironment.Constants.MESSAGE_LEFT; break;
                case 8: advicedAction = AgentEnvironment.Constants.MESSAGE_FORWARD; break;
                case 9: advicedAction = AgentEnvironment.Constants.MESSAGE_FORWARD; break;
                case 10: advicedAction = AgentEnvironment.Constants.MESSAGE_RIGHT; break;
                case 11: advicedAction = AgentEnvironment.Constants.MESSAGE_FORWARD; break;
                case 12: advicedAction = AgentEnvironment.Constants.MESSAGE_CLIMB; break;
            }
            ++time;
            int rand = 1 + (int) (Math.random() * 3);
            switch (rand) {
                case 1: return AgentEnvironment.Constants.ACTION_PROPOSAL1 + advicedAction;
                case 2: return AgentEnvironment.Constants.ACTION_PROPOSAL2 + advicedAction;
                case 3: return AgentEnvironment.Constants.ACTION_PROPOSAL3 + advicedAction;
                default: return "";
            }
        }
    }
}