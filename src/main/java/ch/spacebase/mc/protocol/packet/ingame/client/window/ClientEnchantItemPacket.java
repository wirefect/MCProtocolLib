package ch.spacebase.mc.protocol.packet.ingame.client.window;

import java.io.IOException;

import com.github.steveice10.packetlib.io.NetInput;
import com.github.steveice10.packetlib.io.NetOutput;
import com.github.steveice10.packetlib.packet.Packet;

public class ClientEnchantItemPacket implements Packet {
	
	private int windowId;
	private int enchantment;
	
	@SuppressWarnings("unused")
	private ClientEnchantItemPacket() {
	}
	
	public ClientEnchantItemPacket(int windowId, int enchantment) {
		this.windowId = windowId;
		this.enchantment = enchantment;
	}
	
	public int getWindowId() {
		return this.windowId;
	}
	
	public int getEnchantment() {
		return this.enchantment;
	}

	@Override
	public void read(NetInput in) throws IOException {
		this.windowId = in.readByte();
		this.enchantment = in.readByte();
	}

	@Override
	public void write(NetOutput out) throws IOException {
		out.writeByte(this.windowId);
		out.writeByte(this.enchantment);
	}
	
	@Override
	public boolean isPriority() {
		return false;
	}

}
