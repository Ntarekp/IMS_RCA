package npk.rca.ims.model;

/**
 * TransactionType Enum
 *
 * Represents the two types of stock movements:
 * - IN: Stock coming into inventory (purchases, donations)
 * - OUT: Stock leaving inventory (usage, consumption)
 *
*/
public enum TransactionType {
    /**
     * Stock coming IN (purchases, donations, returns)
     */
    IN,

    /**
     * Stock going OUT (usage, consumption, wastage)
     */
    OUT
}