package com.example.homeworkhoundv2;

import android.util.Log;

import java.util.Date;
import java.util.concurrent.atomic.AtomicInteger;

class AVLNode {
    Assignment assignment;
    AVLNode left;                    // Reference to left child node
    AVLNode right;                   // Reference to right child node
    int height;                      // Height of this node

    public AVLNode(Assignment assignment) {
        this.assignment = assignment;
        this.height = 1;
        this.left = null;
        this.right = null;
    }
}

/*
*
*  Ok so I think the solution to my issues is simple I think what I need to do when modifying assignments
* in the recycler view is in the dialog manager when I modify the assignment I should only have to
* call an update method then notify the adapter back in the assignment adapter.
*/
class AVLTree {
    // Reference to the root node of the tree
    AVLNode root;

    // Constructor
    public AVLTree() {
        // Initialize an empty tree
        root = null;
    }

    /**     Public Methods     **/

    // Insert an assignment into the tree
    public void insert(Assignment assignment) {
        Log.d("AVLTree Debug", "Starting insert");
        root = insertRec(root, assignment);
    }

    // Delete an assignment from the tree
    public void delete(Assignment assignment) {
        root = deleteRec(root, assignment);
    }

    // Modify an assignment in the tree
    public void updateAssignment(Assignment targetAssignment, Assignment updatedAssignment) {
        root = updateAssignmentRec(root, targetAssignment, updatedAssignment);
    }

    // Search for an assignment in the tree
    public AVLNode search(Assignment assignment) {
        // Implement search logic here
        return null; // Return the found node or null if not found
    }

    /** Method to search for an assignment based on the following parameters
     * Parameters:
     *      assignmentName
     *      dueDate
     *      courseID
     * Returns:
     *      The assignment that matches the parameters or null if no match found*/
    public Assignment search(String assignmentNameStr, Date dueDate, String courseIDStr) {
        Assignment temp = new Assignment(assignmentNameStr, dueDate, courseIDStr);
        if (searchByAssignmentRec(root, temp)) {
            return temp;
        }
        else {
            return null;
        }
    }

    public Assignment getAssignmentAtPosition(int position) {
        return getAssignmentAtPositionRec(root, position, new AtomicInteger(0));
    }

    // Public method to get the total number of assignments
    public int getTotalAssignments() {
        AtomicInteger count = new AtomicInteger(0); // To keep track of the count
        countTotalAssignments(root, count);
        return count.get();
    }

    public void printAllAssignments() {
        Log.d("AVLTree Debug", "Start of print all");
        inOrderTraversal(root);
    }

    // Debug method
    public void logTreeStructure() {
        logTreeStructureRec(root, "", true);
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
        //int compare = assignment.getDueDate().compareTo(node.assignment.getDueDate());
        int compare = assignment.compareTo(node.assignment);

        if (compare < 0) {
            // If the given assignment due date is < current node's due date the try insert on the left child
            node.left = insertRec(node.left, assignment);
        }
        else if (compare > 0) {
            // If the given assignment due date is > current node's due date the try insert on the right child
            node.right = insertRec(node.right, assignment);
        }
        else {
            // The due date of the given assignment matches the node's due date. So set the nodes assignment
            // equal to the given assignment
            node.assignment = assignment;

            // The due date of the given assignment matches the node's due date, assignment name, and course ID
            // So I can simply return null because I don't want to insert duplicates.
            //return null;
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
        //int compare = assignment.getDueDate().compareTo(node.assignment.getDueDate());
        int compare = assignment.compareTo(node.assignment);

        if (compare < 0) {
            // If the assignment due date is < current node's due date then try to delete the left child
            node.left = deleteRec(node.left, assignment);
        }
        else if (compare > 0) {
            node.right = deleteRec(node.right, assignment);
        }
        else {
            // Due dates, assignment name, and courseID are the same (Match found)
            if (node.left == null) {
                // If the node's left child node is null, return the right child
                // (The right sub-tree will be used to override the current node, thus deleting the current node)
                return node.right;
            } else if (node.right == null) {
                // If the node's right child node is null, return the left child
                return node.left;
            }

            // If both children are NOT null, find the in-order successor
            AVLNode inOrderSuccessor = minNode(node.right);

            // Override the current node with the in-order successor
            node.assignment = inOrderSuccessor.assignment;

            // Recursively delete the in-order successor from the right subtree
            node.right = removeSmallestNode(node.right);
        }

        // Balance the tree
        node.height = 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
        return balance(node);
    }

    // Method to modify / update and assignment in the tree
    private AVLNode updateAssignmentRec(AVLNode node, Assignment targetAssignment, Assignment updatedAssignment) {
        if (node == null) {
            return null; // Not modifying leaf nodes
        }

        // Compare the given assignment's due date to the due date of the current node
        //int comparator = assignment.getDueDate().compareTo(node.assignment.getDueDate());
        int comparator = targetAssignment.compareTo(node.assignment);

        if (comparator < 0) {
            // If the cur assignment if < 0 then insert to the left
            node.left = updateAssignmentRec(node.left, targetAssignment, updatedAssignment);
        }
        else if (comparator > 0) {
            // If the cur assignment is > 0 then insert to the right
            node.right = updateAssignmentRec(node.right, targetAssignment, updatedAssignment);
        }
        else {
            // Match found
            if (node.assignment.getDueDate().compareTo(updatedAssignment.getDueDate()) == 0) {
                // New and old due dates are the same so I don't need to move any node or balance.
                node.assignment = updatedAssignment;
                return node;
            }
            else {
                // Delete the current node
                root = deleteRec(root, targetAssignment);

                // Insert the a new node with the updated assignment
                root = insertRec(root, updatedAssignment);
            }
        }

        // Update the height and balance the tree
        node.height = 1 + Math.max(calculateHeight(node.left), calculateHeight(node.right));
        return balance(node);
    }

    // Method to search for a specific assignment
    private boolean searchByAssignmentRec(AVLNode node, Assignment targetAssignment) {
        if (node == null) {
            return false; // Ignore Leaf node
        }

        int compare = targetAssignment.compareTo(node.assignment);

        if (compare < 0) {
            // If < 0 search left tree
            return searchByAssignmentRec(node.left, targetAssignment);
        }
        else if (compare > 0) {
            // If > 0 search right tree
            return searchByAssignmentRec(node.right, targetAssignment);
        }
        else {
            // TargetAssignment matches the current node's date, name, and courseID (Match found)
            return true;
        }
    }

    // Method to get the assignment at the given target position. Best case O(1), Worst case O(n)
    private Assignment getAssignmentAtPositionRec(AVLNode node, int targetPosition, AtomicInteger currentPosition) {
        if (node == null) {
            return null; // Assignment not found
        }

        // Traverse the left subtree
        Assignment leftResult = getAssignmentAtPositionRec(node.left, targetPosition, currentPosition);

        if (leftResult != null) {
            return leftResult; // Assignment found in the left subtree
        }

        // Check if the current node is the target position (getAndIncrement() returns the current
        // position then increments)
        if (currentPosition.getAndIncrement() == targetPosition) {
            return node.assignment; // Assignment found
        }

        // Traverse the right subtree
        return getAssignmentAtPositionRec(node.right, targetPosition, currentPosition);
    }

    // Helper method for counting assignments using in-order traversal
    private void countTotalAssignments(AVLNode node, AtomicInteger count) {
        if (node != null) {
            // Traverse left subtree
            countTotalAssignments(node.left, count);

            // Process the current node (in this case, just increment the count)
            count.getAndIncrement();

            // Traverse right subtree
            countTotalAssignments(node.right, count);
        }
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

    // Perform an in-order traversal of the tree and print assignments
    private void inOrderTraversal(AVLNode node) {
        if (node != null) {
            // Traverse the left subtree
            inOrderTraversal(node.left);

            // Process the current node (print assignment details to logcat)
            Log.d("AVLTree Debug", "Assignment Name: " + node.assignment.getAssignmentName());
            Log.d("AVLTree Debug", "Due Date: " + node.assignment.getDueDate());
            Log.d("AVLTree Debug", "Course ID: " + node.assignment.getCourseId());

            // Traverse the right subtree
            inOrderTraversal(node.right);
        }
    }

    // Debug method - Logs the tree structure
    private void logTreeStructureRec(AVLNode node, String prefix, boolean isTail) {
        if (node != null) {
            Log.d("AVLTree Debug", prefix + (isTail ? "└── " : "├── ") + node.assignment.getAssignmentName());

            // Determine the next prefix for child nodes
            String childPrefix = prefix + (isTail ? "    " : "│   ");

            // Log the right subtree
            logTreeStructureRec(node.right, childPrefix, false);

            // Log the left subtree
            logTreeStructureRec(node.left, childPrefix, true);
        }
    }
}
