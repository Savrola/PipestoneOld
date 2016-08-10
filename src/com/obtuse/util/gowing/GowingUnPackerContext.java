package com.obtuse.util.gowing;

import com.obtuse.util.gowing.p2a.GowingEntityReference;
import com.obtuse.util.gowing.p2a.StdGowingTokenizer;
import com.obtuse.util.gowing.p2a.GowingUnPackerParsingException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/*
 * Copyright © 2015 Obtuse Systems Corporation
 */

/**
 Describe something that manages type ids and entity ids during an unpacking operation.
 <p/>Note that the lifespan of a packing context is intended to be the duration of a single unpacking operation.
 */

public interface GowingUnPackerContext {

//    PackingId2 allocatePackingId( EntityTypeName entityTypeName );

//    PackingId2 allocatePackingId( EntityTypeName entityTypeName, long idWithinType );

//    long getHighestPackingIdForType( EntityTypeName entityTypeName );

//    int getOrAllocateTypeReferenceId( Packable2 entity );

//    int getOrAllocateTypeReferenceId( EntityTypeName typeName );

    @Nullable
    Integer findTypeReferenceId( EntityTypeName typeName );

    int getTypeReferenceId( EntityTypeName typeName );

    boolean isEntityKnown( GowingEntityReference er );

    GowingPackable recallPackableEntity( @NotNull GowingEntityReference er );

    Collection<GowingEntityReference> getSeenEntityReferences();

    void rememberPackableEntity( StdGowingTokenizer.GowingToken2 token, GowingEntityReference etr, GowingPackable entity );

//    @NotNull
//    Packable2 getInstance( InstanceId instanceId );

    @NotNull
    Collection<EntityTypeName> getNewTypeNames();

//    @Nullable
//    StdPackingContext2.PackingAssociation findPackingAssociation( InstanceId instanceId );

    @NotNull
    GowingTypeIndex getTypeIndex();

    void clearUnFinishedEntities();

    Collection<GowingEntityReference> getUnfinishedEntities();

    void markEntitiesUnfinished( Collection<GowingEntityReference> unFinishedEntities );

    boolean isEntityFinished( GowingEntityReference er );

    void markEntityFinished( GowingEntityReference er );

    void addUnfinishedEntities( Collection<GowingEntityReference> collection );

    @Nullable
    EntityTypeName findTypeByTypeReferenceId( int typeReferenceId );

    @NotNull
    EntityTypeName getTypeByTypeReferenceId( int typeReferenceId );

    @Nullable
    EntityTypeInfo findTypeInfo( int typeReferenceId );

    @NotNull
    EntityTypeInfo getTypeInfo( int typeReferenceId );

    @Nullable
    EntityTypeInfo findTypeInfo( @NotNull EntityTypeName typeName );

    @NotNull
    EntityTypeInfo getTypeInfo( @NotNull EntityTypeName typeName );

    boolean isTypeNameKnown( EntityTypeName typeName );

    @SuppressWarnings("UnusedReturnValue")
    EntityTypeInfo registerFactory( GowingEntityFactory factory );

//    Collection<InstanceId> getSeenInstanceIds();

    void saveTypeAlias( StdGowingTokenizer.GowingToken2 typeIdToken, StdGowingTokenizer.GowingToken2 typeNameToken )
	    throws GowingUnPackerParsingException;

}