package com.example.homeworkhoundv2;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class AVLNode {
    Date dueDate;                    // Due date of the assignment(s) in this node
    List<Assignment> assignments;    // List of assignments with the same due date
    AVLNode left;                    // Reference to left child node
    AVLNode right;                   // Reference to right child node
    int height;                      // Height of this node

    public AVLNode(Assignment assignment) {
        this.dueDate = assignment.getDueDate();
        this.assignments = new ArrayList<>();
        this.assignments.add(assignment);
        this.height = 1;
        this.left = null;
        this.right = null;
    }
}

// TODO: See if I can make this static so that it is the same AVLTree across all instances(only have to initialize it once)
class AVLTree {
    // Reference to the root node of the tree
    AVLNode root;

    // Constructor
    public AVLTree() {
        // Initialize an empty tree
        root = null;
    }

    // TODO: Add destructor

    /**     Public Methods     **/

    // Insert an assignment into the tree
    public void insert(Assignment assignment) {
        insertRec(root, assignment);
    }

    // Delete an assignment from the tree
    public void delete(Assignment assignment) {
        // Implement deletion logic here
        deleteRec(root, assignment);
    }

    // Search for an assignment in the tree
    public AVLNode search(Assignment assignment) {
        // Implement search logic here
        return null; // Return the found node or null if not found
    }

    // Perform an in-order traversal of the tree
    public void inOrderTraversal() {
        // Implement in-order traversal here
    }

    // Perform a pre-order traversal of the tree
    public void preOrderTraversal() {
        // Implement pre-order traversal here
    }

    // Perform a post-order traversal of the tree
    public void postOrderTraversal() {
        // Implement post-order traversal here
    }

    /**    Private Methods    **/

    // Private method to insert an assignment into the tree
    private AVLNode insertRec(AVLNode node, Assignment assignment) {
        if (node == null) {
            return new AVLNode(assignment);
        }

        // See if the given assignment's due date is <, >,or = to the current node's due date
        int compare = assignment.getDueDate().compareTo(node.dueDate);

        if (compare < 0) {
            // If the given assignment due date is < current node's due date the try insert on the left child
            node.left = insertRec(node.left, assignment);
        }
        else if (compare > 0) {
            // If the given assignment due date is > current node's due date the try insert on the right child
            node.right = insertRec(node.right, assignment);
        }
        else {
            // The due date of the given assignment matches the node's due date. So add the assignment
            // to the assignments list
            node.assignments.add(assignment);
        }

        // Update the height and balance of the tree
        node.height = 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
        return balance(node);
    }

    // private method to delete an assignment from the tree
    private AVLNode deleteRec(AVLNode node, Assignment assignment) {
        if (node == null) {
            return null;
        }

        // See if the given assignment's due date is <, >,or = to the current node's due date
        int compare = assignment.getDueDate().compareTo(node.dueDate);

        if (compare < 0) {
            // If the assignment due date is < current node's due date then try to delete the left child
            node.left = deleteRec(node.left, assignment);
        }
        else if (compare > 0) {
            node.right = deleteRec(node.right, assignment);
        }
        else {
            // Due dates are the same
            // If the node has multiple assignments with the same due date, remove the given assignment
            if (node.assignments.size() > 1) {
                // Remove the given assignment if the assignment is equal to one in the assignment list
                //node.assignments.removeIf(a -> a.equals(assignment));  // option 1
                node.assignments.remove(assignment);                   // option 2
            }
            else {
                // If the node has only one assignment, remove the entire node
                if (node.left == null) {
                    // If the node's left child node is null, return the right child
                    // (The right sub-tree will be used to override the current node, thus deleting the current node)
                    return node.right;
                }
                else if (node.right == null) {
                    // If the node's right child node is null, return the left child
                    return node.left;
                }

                // If both children are NOT null, find the in-order successor
                AVLNode inOrderSuccessor = minNode(node.right);

                // Override the current node with the in-order successor
                node.dueDate = inOrderSuccessor.dueDate;
                node.assignments = inOrderSuccessor.assignments;

                // Recursively delete the in-order successor from the right subtree
                node.right = removeSmallestNode(node.right);
            }
        }

        // Only update the height and balance of the tree when structural change occurs
        if (node.assignments.size() <= 1) {
            node.height = 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
            return balance(node);
        }

        // If the structure doesn't change (no node deleted) then return the current node
        return node;
    }

    // Balance the AVL tree (perform rotations)
    private AVLNode balance(AVLNode node) {
        // Calculate the balance factor of the node
        int balanceFactor = getBalanceFactor(node);

        // Check if node is unbalanced and perform any necessary rotations
        if (balanceFactor > 1) {
            // The node is left heavy (aka left-left or left-right case)
            if (getBalanceFactor(node.left) >= 0) {
                // Left-left case: Perform right rotation on the current node
                return rightRotate(node);
            }
            else {
                // Left-Right Case: Perform a left rotation on the left child followed by a right rotation on the current node
                node.left = leftRotate(node.left);
                return rightRotate(node);
            }
        }
        else if (balanceFactor < -1) {
            // Right heavy: right-right or right-left case
            if (getBalanceFactor(node.right) <= 0) {
                // Right-right case: So simply rotate left
                return leftRotate(node);
            }
            else {
                // Right-left case: So perform a right rotation on the right child of node then left rotation on node
                node.right = rightRotate(node.right);  // Right rotation on right child node
                return leftRotate(node);               // Left rotation on left
            }
        }

        return node; // Return the balanced node
    }

    // Get the balance factor of the node
    private int getBalanceFactor(AVLNode node) {
        // If the node is null then it doesn't have any factor ( or weight)
        if (node == null) {
            return 0;
        }

        // Else calculate the balance factor by finding the height difference in the left and right sub-trees
        return calculateHeight(node.left) - calculateHeight(node.right);
    }

    // Calculate the height of a node
    private int calculateHeight(AVLNode node) {
        // If the node is null it has a height of 0
        if (node == null) {
            return 0;
        }

        // Else return the height of the existing node
        return node.height;
    }

    // Find the node with the minimum value in the tree
    private AVLNode minNode(AVLNode node) {
        // Navigate down the left tree until the left node is null
        while (node.left != null) {
            // Navigate to the next node in the left sub tree
            node = node.left;
        }

        // Return the smallest node found
        return node;
    }

    // Perform a right rotation on a node
    private AVLNode rightRotate(AVLNode y) {
        // y is the node the rotation will be performed on
        AVLNode x = y.left;   // x is the left child node that will take y's place
        AVLNode T2 = x.right; // T2 is the right child of x

        // Rotate the nodes
        x.right = y; // Make x the parent of y (rotates the y sub-tree to be a right child of x) (y still > x)
        y.left = T2; // Make T2 the left child of y (because T2 is greater than x but less then y)

        // Update the heights of y and x
        y.height = Math.max(calculateHeight(y.left), calculateHeight(y.right)) + 1;
        x.height = Math.max(calculateHeight(x.left), calculateHeight(x.right)) + 1;
        /* Side Note: the height of a node tells you how many edges / branches are between it and the leaf node(farthest node) */

        // Return the new root of the subtree
        return x;
    }

    // Perform a left rotation on a node
    private AVLNode leftRotate(AVLNode x) {
        // x is the node I will be performing the rotation on
        AVLNode y = x.right; // y is the node that will take x's spot (rotates left to x's position)
        AVLNode T2 = y.left; // T2 is the left child of y

        // Perform the rotation
        y.left = x;    // Make the x node the left child of y ( y is > x)
        x.right = T2;  // Make T2 the right child of x (because T2 is < y but > x)

        // Update the height of x and y
        x.height = Math.max(calculateHeight(x.left), calculateHeight(x.right)) + 1;
        y.height = Math.max(calculateHeight(y.left), calculateHeight(y.right)) + 1;

        // Return the new root of the subtree
        return y;
    }

    // Method to delete the entire given sub tree using the given node as the root
    private AVLNode deleteSubtree(AVLNode node) {
        if (node == null) {
            return null;
        }

        // Recursively delete the left and right subtrees
        node.left = deleteSubtree(node.left);
        node.right = deleteSubtree(node.right);

        // This touches every non-null node and essentially sets it to null
        return null;
    }

    //
    private AVLNode removeSmallestNode(AVLNode node) {
        if (node.left == null) {
            // If the left child is null, this is the smallest node
            // Return the right child, effectively removing this node (and replacing it with the right child)
            return node.right;
        }

        // Recursively remove the smallest node from the left subtree
        node.left = removeSmallestNode(node.left);

        // Update the height and balance of the tree (Occurs after all recursion is done)
        node.height = 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
        return balance(node);
    }
}
