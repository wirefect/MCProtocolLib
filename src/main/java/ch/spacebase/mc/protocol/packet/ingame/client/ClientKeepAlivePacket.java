package ch.spacebase.mc.protocol.packet.ingame.client;

import java.io.IOException;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;

public class ClientKeepAlivePacket implements Packet {
	
	private int id;
	
	@SuppressWarnings("unused")
	private ClientKeepAlivePacket() {
	}
	
	public ClientKeepAlivePacket(int id) {
		this.id = id;
	}
	
	public int getPingId() {
		return this.id;
	}

	@Override
	public void read(NetInput in) throws IOException {
		this.id = in.readInt();
	}

	@Override
	public void write(NetOutput out) throws IOException {
		out.writeInt(this.id);
	}
	
	@Override
	public boolean isPriority() {
		return false;
	}

}
