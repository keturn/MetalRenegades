/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.terasology.metalrenegades.economy.ui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.terasology.assets.ResourceUrn;
import org.terasology.economy.events.ResourceInfoRequestEvent;
import org.terasology.entitySystem.entity.EntityManager;
import org.terasology.entitySystem.entity.EntityRef;
import org.terasology.entitySystem.event.ReceiveEvent;
import org.terasology.entitySystem.systems.BaseComponentSystem;
import org.terasology.entitySystem.systems.RegisterMode;
import org.terasology.entitySystem.systems.RegisterSystem;
import org.terasology.logic.characters.interactions.InteractionUtil;
import org.terasology.logic.inventory.InventoryManager;
import org.terasology.logic.players.LocalPlayer;
import org.terasology.metalrenegades.economy.events.MarketScreenRequestEvent;
import org.terasology.metalrenegades.economy.events.TransactionType;
import org.terasology.registry.In;
import org.terasology.registry.Share;
import org.terasology.rendering.nui.NUIManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Populates the market screen
 */
@Share(MarketUISystem.class)
@RegisterSystem(RegisterMode.CLIENT)
public class MarketUISystem extends BaseComponentSystem {

    @In
    private NUIManager nuiManager;

    @In
    private EntityManager entityManager;

    @In
    private InventoryManager inventoryManager;

    @In
    private LocalPlayer localPlayer;

    private Logger logger = LoggerFactory.getLogger(MarketUISystem.class);

    private MarketScreen marketScreen;

    @Override
    public void initialise() {
        marketScreen = (MarketScreen) nuiManager.createScreen("MetalRenegades:marketScreen");
    }

    @ReceiveEvent
    public void onToggleInventory(MarketScreenRequestEvent event, EntityRef entity) {
        ResourceUrn activeInteractionScreenUri = InteractionUtil.getActiveInteractionScreenUri(entity);
        if (activeInteractionScreenUri != null) {
            InteractionUtil.cancelInteractionAsClient(entity);
        }

        nuiManager.toggleScreen("MetalRenegades:marketScreen");
    }

    @ReceiveEvent
    public void onMarketScreenAction(MarketScreenRequestEvent event, EntityRef entityRef) {
        List<MarketItem> marketItemList = new ArrayList<>();

        if (event.type == TransactionType.BUYING) {
            EntityRef market = entityManager.getEntity(event.market);
            ResourceInfoRequestEvent resourceInfoRequestEvent = new ResourceInfoRequestEvent();
            Map<String, Integer> resources;
            market.send(resourceInfoRequestEvent);

            if (resourceInfoRequestEvent.isHandled) {
                resources = resourceInfoRequestEvent.resources;
            } else {
                logger.error("Could not retrieve resource information.");
                return;
            }

            for (Map.Entry<String, Integer> entry : resources.entrySet()) {
                MarketItem item = MarketItemBuilder.get(entry.getKey(), entry.getValue());
                marketItemList.add(item);
            }
        } else if (event.type == TransactionType.SELLING){
            EntityRef player = localPlayer.getCharacterEntity();
            int slots = inventoryManager.getNumSlots(player);

            for (int i = 0; i < slots; i++) {
                EntityRef entity = inventoryManager.getItemInSlot(player, i);
                if (entity.getParentPrefab() != null) {
                    MarketItem item = MarketItemBuilder.get(entity.getParentPrefab().getName(), 1); // TODO: 1?
                    marketItemList.add(item);
                }
            }
        } else {
            logger.warn("TransactionType not recognised.");
        }

        marketScreen.setType(event.type);
        marketScreen.setItemList(marketItemList);
    }
}
