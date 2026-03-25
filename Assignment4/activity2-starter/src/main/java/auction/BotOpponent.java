package auction;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Bot opponent that bids against the player.
 * Students do not need to modify this class.
 */
public class BotOpponent {
    private String name;
    private int gold;
    private List<Item> inventory;
    private Random random;
    private boolean useGradingMode;

    /**
     * Create a bot opponent with a given name.
     */
    public BotOpponent(String name, boolean gradingMode) {
        this.name = name;
        this.gold = 150;
        this.inventory = new ArrayList<>();
        this.useGradingMode = gradingMode;

        if (gradingMode) {
            // Use seeded random for grading mode (deterministic)
            this.random = new Random(name.hashCode());
        } else {
            this.random = new Random();
        }
    }

    /**
     * Decide how much to bid on an item (no reserve price check).
     * Uses a simple strategy: bid between 40-70% of the average value.
     */
    public int decideBid(Item item) {
        return decideBid(item, 0);
    }

    /**
     * Decide how much to bid on an item, respecting reserve price.
     * Uses a simple strategy: bid between 40-70% of the average value.
     * If the calculated bid is below reserve price, bot bids 0 (skips).
     */
    public int decideBid(Item item, int reservePrice) {
        if (gold == 0) {
            return 0;
        }

        // Calculate target bid based on item's value range
        int avgValue = (item.getMinValue() + item.getMaxValue()) / 2;
        int minBid = (avgValue * 40) / 100;
        int maxBid = (avgValue * 70) / 100;

        // Don't bid more than we have
        maxBid = Math.min(maxBid, gold);
        minBid = Math.min(minBid, gold);

        // Ensure valid range
        if (minBid > maxBid) {
            minBid = maxBid;
        }

        // Sometimes bid 0 (10% chance)
        if (!useGradingMode && random.nextInt(10) == 0) {
            return 0;
        }

        // Bid random amount between min and max
        int bid;
        if (minBid == maxBid) {
            bid = minBid;
        } else {
            bid = minBid + random.nextInt(maxBid - minBid + 1);
        }

        // Respect reserve price: if bid is below reserve, skip (bid 0)
        if (bid > 0 && bid < reservePrice) {
            // Try to bid at reserve price if we can afford it
            if (reservePrice <= gold) {
                return reservePrice;
            }
            return 0;
        }

        return bid;
    }

    /**
     * Award an item to this bot opponent.
     */
    public void awardItem(Item item, int bidAmount) {
        inventory.add(item);
        gold -= bidAmount;
    }

    /**
     * Calculate total value of inventory.
     */
    public int getInventoryValue() {
        int total = 0;
        for (Item item : inventory) {
            total += item.getActualValue();
        }
        return total;
    }

    /**
     * Calculate final score (gold + inventory value).
     */
    public int getTotalScore() {
        return gold + getInventoryValue();
    }

    // Getters
    public String getName() {
        return name;
    }

    public int getGold() {
        return gold;
    }

    public List<Item> getInventory() {
        return new ArrayList<>(inventory);
    }

    public List<String> getItemNames() {
        List<String> names = new ArrayList<>();
        for (Item item : inventory) {
            names.add(item.getName());
        }
        return names;
    }
}
