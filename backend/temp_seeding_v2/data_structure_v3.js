// Version 3
// 数据结构：链表实现 / Data Structure: Linked List Implementation
// 这是一个单向链表的JavaScript实现 / This is a JavaScript implementation of a singly linked list

class Node {
    constructor(data) {
        this.data = data;      // 节点数据 / Node data
        this.next = null;      // 下一个节点 / Next node
    }
}

class LinkedList {
    constructor() {
        this.head = null;      // 链表头 / List head
        this.size = 0;         // 链表大小 / List size
    }
    
    // 在末尾添加节点 / Add node at the end
    append(data) {
        const newNode = new Node(data);
        
        if (!this.head) {
            this.head = newNode;
        } else {
            let current = this.head;
            while (current.next) {
                current = current.next;
            }
            current.next = newNode;
        }
        this.size++;
    }
    
    // 打印链表 / Print the list
    print() {
        let current = this.head;
        let result = [];
        while (current) {
            result.push(current.data);
            current = current.next;
        }
        console.log("链表内容 / List content:", result.join(' -> '));
    }
}

// 测试 / Test
const list = new LinkedList();
list.append(10);
list.append(20);
list.append(30);
list.print();
