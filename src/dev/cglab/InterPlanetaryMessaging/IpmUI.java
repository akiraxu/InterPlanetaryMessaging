package dev.cglab.InterPlanetaryMessaging;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.*;

public class IpmUI {
	
	public IpmServer server;
	
	public JFrame frame;
	public JTextArea messageView;
	
	public IpmUI(IpmServer s) {
		
		server = s;
		
        frame = new JFrame("Inter Planetary Messaging");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(800, 400);

        JPanel sendPanel = new JPanel();
        JLabel userid_label = new JLabel("Send To / Checks");
        JTextField userid = new JTextField(10);
        JLabel text_label = new JLabel("Text");
        JTextField text = new JTextField(20);
        JButton send = new JButton("Send");
        JButton check = new JButton("Check");
        JButton post = new JButton("Post");
        send.addActionListener(new ActionListener(){  
        	@Override
			public void actionPerformed(ActionEvent arg0) {
				server.uiSend(userid.getText().trim(), text.getText().trim());
			}  
        });
        check.addActionListener(new ActionListener(){  
        	@Override
			public void actionPerformed(ActionEvent arg0) {
				server.uiCheck(userid.getText().trim());
			}  
        });
        post.addActionListener(new ActionListener(){  
        	@Override
			public void actionPerformed(ActionEvent arg0) {
				server.uiPost(text.getText().trim());
			}  
        });
        sendPanel.add(userid_label);
        sendPanel.add(userid);
        sendPanel.add(text_label);
        sendPanel.add(text);
        sendPanel.add(send);
        sendPanel.add(check);
        sendPanel.add(post);
        
        messageView = new JTextArea();
        messageView.setLineWrap(true);

        frame.getContentPane().add(BorderLayout.SOUTH, sendPanel);
        frame.getContentPane().add(BorderLayout.CENTER, messageView);
        frame.setVisible(true);
	}
	
	public void append(String text) {
		messageView.append(text + "\n");
	}
}
