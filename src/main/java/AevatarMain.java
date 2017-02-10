

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.WindowConstants;

import xdi2.agent.impl.XDIBasicAgent;
import xdi2.agent.routing.XDIAgentRouter;
import xdi2.agent.routing.impl.bootstrap.XDIBootstrapLocalAgentRouter;
import xdi2.agent.routing.impl.local.XDILocalAgentRouter;
import xdi2.client.impl.local.XDILocalClient;
import xdi2.core.ContextNode;
import xdi2.core.Graph;
import xdi2.core.features.aggregation.Aggregation;
import xdi2.core.features.linkcontracts.instance.RootLinkContract;
import xdi2.core.impl.memory.MemoryGraphFactory;
import xdi2.core.syntax.XDIAddress;
import xdi2.core.util.CopyUtil;
import xdi2.messaging.Message;
import xdi2.messaging.MessageEnvelope;
import xdi2.messaging.container.impl.graph.GraphMessagingContainer;
import xdi2.messaging.container.interceptor.impl.connect.ConnectInterceptor;
import xdi2.messaging.container.interceptor.impl.security.signature.SignatureInterceptor;
import xdi2.messaging.container.interceptor.impl.send.SendInterceptor;
import xdi2.messaging.response.TransportMessagingResponse;

public class AevatarMain extends AevatarMainUI {

	public AevatarMain() {

		super();
		initComponents();
		try {
			initXdi();
		} catch (Exception ex) {
			this.error(ex);
		}
	}

	private void initComponents() {

		Util.initJFrame(this);
	}

	private Graph aliceGraph;
	private XDILocalClient aliceLocalClient;

	private void initXdi() throws Exception {

		aliceGraph = MemoryGraphFactory.getInstance().openGraph();
		GraphMessagingContainer aliceContainer = new GraphMessagingContainer(aliceGraph);
		aliceContainer.addStandardExtensions();
		XDIBasicAgent aliceAgent = new XDIBasicAgent(new XDIAgentRouter<?, ?> [] { new XDIBootstrapLocalAgentRouter(), new XDILocalAgentRouter(aliceContainer) } );
		aliceContainer.getInterceptors().findInterceptor(SendInterceptor.class).setXdiAgent(aliceAgent);
		aliceContainer.getInterceptors().findInterceptor(ConnectInterceptor.class).setXdiAgent(aliceAgent);
		aliceContainer.getInterceptors().removeInterceptor(aliceContainer.getInterceptors().findInterceptor(SignatureInterceptor.class));

		// XDI-DATA-ALICE-1: Self-asserted basic profile data

		CopyUtil.copyGraph(MemoryGraphFactory.getInstance().loadGraph(new File("xdi-data-alice-1.xdi")), aliceGraph, null);

		// XDI-DATA-ALICE-2: Linking unlisted identifier to Ævatar ID

		CopyUtil.copyGraph(MemoryGraphFactory.getInstance().loadGraph(new File("xdi-data-alice-2.xdi")), aliceGraph, null);

		// XDI-DATA-ALICE-3: Standard link contract that enables first contact

		CopyUtil.copyGraph(MemoryGraphFactory.getInstance().loadGraph(new File("xdi-data-alice-3.xdi")), aliceGraph, null);

		// XDI-DATA-ALICE-5: Medical records in Alice's identity container

		CopyUtil.copyGraph(MemoryGraphFactory.getInstance().loadGraph(new File("xdi-data-alice-5.xdi")), aliceGraph, null);

		// prepare transport to execute messages against container

		aliceLocalClient = new XDILocalClient(aliceContainer);
	}

	protected void aliceViewIdentityContainerActionPerformed(ActionEvent e) {

		xdi(this.aliceGraph);
	}

	protected void doctorSendRequestMedicalRecordsActionPerformed(ActionEvent e) {

		try {

			// XDI-MSG-5: send request for medical records to Alice's identity container

			MessageEnvelope me = MessageEnvelope.fromGraph(MemoryGraphFactory.getInstance().loadGraph(new File("xdi-msg-5.xdi")));

			TransportMessagingResponse response = aliceLocalClient.send(me);

			// display to the user

			xdi(response.getGraph());
		} catch (Exception ex) {

			error(ex);
		}
	}

	private List<Message> pendingMessages;

	protected void aliceViewPendingMessagesActionPerformed(ActionEvent e) {

		try {

			// look up pending messages in Alice's identity container

			MessageEnvelope me = new MessageEnvelope();
			Message m = me.createMessage(Constants.XDI_ADD_ALICE_AVATAR_ID);
			m.setToXDIAddress(Constants.XDI_ADD_ALICE_AVATAR_ID);
			m.setLinkContractClass(RootLinkContract.class);

			m.createGetOperation(XDIAddress.create("[$msg]"));

			TransportMessagingResponse response = aliceLocalClient.send(me);

			// extract and remember incoming requests in Alice's identity container

			pendingMessages = new ArrayList<Message> ();
			ContextNode pendingMessagesContextNodes = response.getResultGraph().getDeepContextNode(XDIAddress.create("[$msg]"));
			for (ContextNode penndingMessageContextNode : Aggregation.getAggregationContextNodes(pendingMessagesContextNodes)) {

				Message pendingMessage = Message.fromContextNode(penndingMessageContextNode);
				pendingMessages.add(pendingMessage);
			}

			// display to the user

			xdi(response.getGraph());
		} catch (Exception ex) {

			error(ex);
		}
	}

	protected void aliceModifyIncomingRequestActionPerformed(ActionEvent e) {
		// TODO add your code here
	}

	protected void aliceApprovePendingMessagesActionPerformed(ActionEvent e) {

		try {

			// approve pending messages in Alice's identity container

			MessageEnvelope me = new MessageEnvelope();
			Message m = me.createMessage(Constants.XDI_ADD_ALICE_AVATAR_ID);
			m.setToXDIAddress(Constants.XDI_ADD_ALICE_AVATAR_ID);
			m.setLinkContractClass(RootLinkContract.class);

			for (Message pendingMessage : pendingMessages) {

				m.createSendOperation(pendingMessage);
				m.createDelOperation(pendingMessage.getContextNode().getXDIAddress());
			}

			TransportMessagingResponse response = aliceLocalClient.send(me);

			// display to the user

			xdi(response.getGraph());
		} catch (Exception ex) {

			error(ex);
		}
	}

	protected void aliceRejectPendingMessagesActionPerformed(ActionEvent e) {

		try {

			// delete pending messages in Alice's identity container

			MessageEnvelope me = new MessageEnvelope();
			Message m = me.createMessage(Constants.XDI_ADD_ALICE_AVATAR_ID);
			m.setToXDIAddress(Constants.XDI_ADD_ALICE_AVATAR_ID);
			m.setLinkContractClass(RootLinkContract.class);

			for (Message pendingMessage : pendingMessages) {

				m.createDelOperation(pendingMessage.getContextNode().getXDIAddress());
			}

			TransportMessagingResponse response = aliceLocalClient.send(me);

			// display to the user

			xdi(response.getGraph());
		} catch (Exception ex) {

			error(ex);
		}
	}

	protected void doctorSendNewMedicalRecordActionPerformed(ActionEvent e) {

		try {

			// XDI-MSG-7: add new medical record to Alice's identity container

			MessageEnvelope me = MessageEnvelope.fromGraph(MemoryGraphFactory.getInstance().loadGraph(new File("xdi-msg-7.xdi")));

			TransportMessagingResponse response = aliceLocalClient.send(me);

			// display to the user

			xdi(response.getGraph());
		} catch (Exception ex) {

			error(ex);
		}
	}

	private void xdi(Graph g) {

		JFrame frame = new AevatarXdi(g);
		frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		frame.setVisible(true);
	}

	private void error(Exception ex) {

		JOptionPane.showMessageDialog(null, (ex.getMessage() != null && ex.getMessage().length() > 1) ? ex.getMessage() : ex.getClass().getName());
	}
}
