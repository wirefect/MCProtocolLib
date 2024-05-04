package org.geysermc.mcprotocollib.network.packet;

import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import org.geysermc.mcprotocollib.network.Server;
import org.geysermc.mcprotocollib.network.Session;
import org.geysermc.mcprotocollib.network.codec.ByteBufWrapper;
import org.geysermc.mcprotocollib.network.codec.CodecByteBuf;
import org.geysermc.mcprotocollib.network.codec.PacketDefinition;
import org.geysermc.mcprotocollib.network.codec.PacketSerializer;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A protocol for packet sending and receiving.
 * All implementations must have a constructor that takes in a {@link ByteBuf}.
 */
public abstract class PacketProtocol<B extends CodecByteBuf> {
    private final Int2ObjectMap<PacketDefinition<? extends Packet, B>> serverbound = new Int2ObjectOpenHashMap<>();
    private final Int2ObjectMap<PacketDefinition<? extends Packet, B>> clientbound = new Int2ObjectOpenHashMap<>();

    private final Map<Class<? extends Packet>, Integer> clientboundIds = new IdentityHashMap<>();
    private final Map<Class<? extends Packet>, Integer> serverboundIds = new IdentityHashMap<>();

    /**
     * Gets the prefix used when locating SRV records for this protocol.
     *
     * @return The protocol's SRV record prefix.
     */
    public abstract String getSRVRecordPrefix();

    /**
     * Gets the packet header of this protocol.
     *
     * @return The protocol's packet header.
     */
    public abstract PacketHeader getPacketHeader();

    /**
     * Creates a new {@link ByteBufWrapper} that can be used
     * for each session.
     *
     * @return A new {@link ByteBufWrapper}.
     */
    public abstract ByteBufWrapper<B> getByteBufWrapper();

    /**
     * Called when a client session is created with this protocol.
     *
     * @param session The created session.
     * @param transferring If the client is being transferred between servers.
     */
    public abstract void newClientSession(Session session, boolean transferring);

    /**
     * Called when a server session is created with this protocol.
     *
     * @param server The server that the session belongs to.
     * @param session The created session.
     */
    public abstract void newServerSession(Server server, Session session);

    /**
     * Clears all currently registered packets.
     */
    public final void clearPackets() {
        this.serverbound.clear();
        this.clientbound.clear();
        this.clientboundIds.clear();
        this.serverboundIds.clear();
    }

    /**
     * Registers a packet to this protocol as both serverbound and clientbound.
     *
     * @param id Id to register the packet to.
     * @param packet Packet to register.
     * @param serializer The packet serializer.
     * @throws IllegalArgumentException If the packet fails a test creation when being registered as serverbound.
     */
    public final <T extends Packet> void register(int id, Class<T> packet, PacketSerializer<T, B> serializer) {
        this.registerServerbound(id, packet, serializer);
        this.registerClientbound(id, packet, serializer);
    }

    /**
     * Registers a packet to this protocol as both serverbound and clientbound.
     *
     * @param definition The packet definition.
     * @throws IllegalArgumentException If the packet fails a test creation when being registered as serverbound.
     */
    public final void register(PacketDefinition<? extends Packet, B> definition) {
        this.registerServerbound(definition);
        this.registerClientbound(definition);
    }

    /**
     * Registers a serverbound packet to this protocol.
     *
     * @param id Id to register the packet to.
     * @param packet Packet to register.
     * @param serializer The packet serializer.
     * @throws IllegalArgumentException If the packet fails a test creation.
     */
    public final <T extends Packet> void registerServerbound(int id, Class<T> packet, PacketSerializer<T, B> serializer) {
        this.registerServerbound(new PacketDefinition<>(id, packet, serializer));
    }

    /**
     * Registers a serverbound packet to this protocol.
     *
     * @param definition The packet definition.
     */
    public final void registerServerbound(PacketDefinition<? extends Packet, B> definition) {
        this.serverbound.put(definition.getId(), definition);
        this.serverboundIds.put(definition.getPacketClass(), definition.getId());
    }

    /**
     * Registers a clientbound packet to this protocol.
     *
     * @param id Id to register the packet to.
     * @param packet Packet to register.
     * @param serializer The packet serializer.
     */
    public final <T extends Packet> void registerClientbound(int id, Class<T> packet, PacketSerializer<T, B> serializer) {
        this.registerClientbound(new PacketDefinition<>(id, packet, serializer));
    }

    /**
     * Registers a clientbound packet to this protocol.
     *
     * @param definition The packet definition.
     */
    public final void registerClientbound(PacketDefinition<? extends Packet, B> definition) {
        this.clientbound.put(definition.getId(), definition);
        this.clientboundIds.put(definition.getPacketClass(), definition.getId());
    }

    public Packet createClientboundPacket(int id, ByteBuf buf) {
        return createClientboundPacket(id, getByteBufWrapper().wrap(buf));
    }

    /**
     * Creates a new instance of a clientbound packet with the given id and read the clientbound input.
     *
     * @param id Id of the packet to create.
     * @param buf The buffer to read the packet from.
     * @return The created packet.
     * @throws IllegalArgumentException If the packet ID is not registered.
     */
    public Packet createClientboundPacket(int id, B buf) {
        PacketDefinition<?, B> definition = this.clientbound.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        return definition.newInstance(buf);
    }

    /**
     * Gets the registered id of a clientbound packet class.
     *
     * @param packetClass Class of the packet to get the id for.
     * @return The packet's registered id.
     * @throws IllegalArgumentException If the packet is not registered.
     */
    public int getClientboundId(Class<? extends Packet> packetClass) {
        Integer packetId = this.clientboundIds.get(packetClass);
        if (packetId == null) {
            throw new IllegalArgumentException("Unregistered clientbound packet class: " + packetClass.getName());
        }

        return packetId;
    }

    /**
     * Gets the registered id of a clientbound {@link Packet} instance.
     *
     * @param packet Instance of {@link Packet} to get the id for.
     * @return The packet's registered id.
     * @throws IllegalArgumentException If the packet is not registered.
     */
    public int getClientboundId(Packet packet) {
        return getClientboundId(packet.getClass());
    }

    /**
     * Gets the packet class for a packet id.
     *
     * @param id The packet id.
     * @return The registered packet's class
     * @throws IllegalArgumentException If the packet ID is not registered.
     */
    public Class<? extends Packet> getClientboundClass(int id) {
        PacketDefinition<?, ?> definition = this.clientbound.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        return definition.getPacketClass();
    }

    public Packet createServerboundPacket(int id, ByteBuf buf) {
        return createServerboundPacket(id, getByteBufWrapper().wrap(buf));
    }

    /**
     * Creates a new instance of a serverbound packet with the given id and read the serverbound input.
     *
     * @param id Id of the packet to create.
     * @param buf The buffer to read the packet from.
     * @return The created packet.
     * @throws IllegalArgumentException If the packet ID is not registered.
     */
    public Packet createServerboundPacket(int id, B buf) {
        PacketDefinition<?, B> definition = (PacketDefinition<?, B>) this.serverbound.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        return definition.newInstance(buf);
    }

    /**
     * Gets the registered id of a serverbound packet class.
     *
     * @param packetClass Class of the packet to get the id for.
     * @return The packet's registered id.
     * @throws IllegalArgumentException If the packet is not registered.
     */
    public int getServerboundId(Class<? extends Packet> packetClass) {
        Integer packetId = this.serverboundIds.get(packetClass);
        if (packetId == null) {
            throw new IllegalArgumentException("Unregistered serverbound packet class: " + packetClass.getName());
        }

        return packetId;
    }

    /**
     * Gets the registered id of a serverbound {@link Packet} instance.
     *
     * @param packet Instance of {@link Packet} to get the id for.
     * @return The packet's registered id.
     * @throws IllegalArgumentException If the packet is not registered.
     */
    public int getServerboundId(Packet packet) {
        return getServerboundId(packet.getClass());
    }

    /**
     * Gets the packet class for a packet id.
     *
     * @param id The packet id.
     * @return The registered packet's class
     * @throws IllegalArgumentException If the packet ID is not registered.
     */
    public Class<? extends Packet> getServerboundClass(int id) {
        PacketDefinition<?, ?> definition = this.serverbound.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        return definition.getPacketClass();
    }

    /**
     * Gets the serverbound packet definition for the given packet id.
     *
     * @param id The packet id.
     * @return The registered packet's class
     */
    public PacketDefinition<?, B> getServerboundDefinition(int id) {
        PacketDefinition<?, B> definition = this.serverbound.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        return definition;
    }

    /**
     * Gets the clientbound packet definition for the given packet id.
     *
     * @param id The packet id.
     * @return The registered packet's class
     */
    public PacketDefinition<?, B> getClientboundDefinition(int id) {
        PacketDefinition<?, B> definition = this.clientbound.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Invalid packet id: " + id);
        }

        return definition;
    }
}
