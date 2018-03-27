//
//  ========================================================================
//  Copyright (c) 1995-2018 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.http2;

/**
 * <pre>
 *            NOT_CLOSED
 *              /    \            
 *             /      \ gen            
 *        rcv /        \eos     
 *        eos/          \
 *          /            \                     
 *         /              \      
 *        v                v        
 * REMOTELY_CLOSED  LOCALLY_CLOSING        
 *        |          /      |                    
 *        |         /       |                       
 *     gen|        /rcv     |snd                    
 *     eos|       / eos     |eos                  
 *        |      /          |    
 *        |     /           |        
 *        v    v            |         
 *       CLOSING    LOCALLY_CLOSED             
 *         \               /          
 *          \             /           
 *        snd\           /rcv            
 *        eos \         / eos            
 *             \       /              
 *              \     /               
 *               v   v
 *               CLOSED                             
 * </pre>
 */
public enum CloseState
{
    NOT_CLOSED,      // Stream is open
    LOCALLY_CLOSING, // An end of stream frame has been generated. 
    LOCALLY_CLOSED,  // An end of stream frame has been generated and sent. 
    REMOTELY_CLOSED, // An end of stream frame has been received
    CLOSING,         // An end of stream frame has been received and generated, but not yet sent.
    CLOSED           // An end of stream frame has been received, generated and sent
}
