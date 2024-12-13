import fi.iki.elonen.NanoHTTPD;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MemoryAllocationServer extends NanoHTTPD {

    // List to store memory blocks
    private final List<MemoryBlock> memoryBlocks;

    public MemoryAllocationServer() throws IOException {
        super(8080); // UI server
        memoryBlocks = initializeMemoryBlocks();
        start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
        System.out.println("Server is running at http://localhost:8080");
    }

    // MemoryBlock class to represent each memory block
    private static class MemoryBlock {
        int size;
        boolean isAllocated;

        MemoryBlock(int size) {
            this.size = size;
            this.isAllocated = false;
        }

        @Override
        public String toString() {
            return "{ size: " + size + ", allocated: " + isAllocated + " }";
        }
    }

    // Initialize memory blocks with some default sizes
    private List<MemoryBlock> initializeMemoryBlocks() {
        List<MemoryBlock> blocks = new ArrayList<>();
        blocks.add(new MemoryBlock(100));
        blocks.add(new MemoryBlock(250));
        blocks.add(new MemoryBlock(300));
        blocks.add(new MemoryBlock(325));
        return blocks;
    }

    @Override
    public Response serve(IHTTPSession session) {
        String uri = session.getUri();

        if ("/".equals(uri)) {
            return newFixedLengthResponse(Response.Status.OK, "text/html", loadHTML("memory_ui.html"));
        } else if ("/allocate".equals(uri)) {
            Map<String, String> params = session.getParms();
            String sizeParam = params.get("size");

            if (sizeParam != null) {
                try {
                    int requestSize = Integer.parseInt(sizeParam);
                    String result = allocateMemory(requestSize);
                    return newFixedLengthResponse(result);
                } catch (NumberFormatException e) {
                    return newFixedLengthResponse("Invalid size format!");
                }
            }
        } else if ("/deallocate".equals(uri)) {
            Map<String, String> params = session.getParms();
            String indexParam = params.get("index");

            if (indexParam != null) {
                try {
                    int blockIndex = Integer.parseInt(indexParam);
                    String result = deallocateMemory(blockIndex);
                    return newFixedLengthResponse(result);
                } catch (NumberFormatException e) {
                    return newFixedLengthResponse("Invalid block index format!");
                }
            }
        } else if ("/memory-status".equals(uri)) {
            // Return memory block status as JSON
            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < memoryBlocks.size(); i++) {
                MemoryBlock block = memoryBlocks.get(i);
                json.append("{")
                        .append("\"index\":").append(i).append(",")
                        .append("\"size\":").append(block.size).append(",")
                        .append("\"isAllocated\":").append(block.isAllocated)
                        .append("}");
                if (i < memoryBlocks.size() - 1) json.append(",");
            }
            json.append("]");
            return newFixedLengthResponse(Response.Status.OK, "application/json", json.toString());
        }

        return newFixedLengthResponse(Response.Status.NOT_FOUND, "text/plain", "404 Not Found");
    }


    // Load the HTML file from resources
    private String loadHTML(String filePath) {
        try {
            return new String(java.nio.file.Files.readAllBytes(java.nio.file.Paths.get("src/main/resources/" + filePath)));
        } catch (IOException e) {
            return "<h1>Error loading HTML</h1>";
        }
    }

    // Allocate memory using the Worst Fit algorithm
    private String allocateMemory(int requestSize) {
        MemoryBlock worstBlock = null;
        int worstBlockIndex = -1;

        // Find the largest block that can store the process
        for (int i = 0; i < memoryBlocks.size(); i++) {
            MemoryBlock block = memoryBlocks.get(i);
            if (!block.isAllocated && block.size >= requestSize) {
                if (worstBlock == null || block.size > worstBlock.size) {
                    worstBlock = block;
                    worstBlockIndex = i;
                }
            }
        }

        if (worstBlock != null) {
            worstBlock.isAllocated = true;
            return "Allocated " + requestSize + "Kb to block at index " + worstBlockIndex + " (size " + worstBlock.size + "Kb)";
        } else {
            return "Allocation failed: No suitable block available.";
        }
    }

    // Deallocate memory from a specific block
    private String deallocateMemory(int blockIndex) {
        if (blockIndex >= 0 && blockIndex < memoryBlocks.size()) {
            MemoryBlock block = memoryBlocks.get(blockIndex);
            if (block.isAllocated) {
                block.isAllocated = false;
                return "Block " + blockIndex + " deallocated successfully.";
            } else {
                return "Block " + blockIndex + " is already free.";
            }
        }
        return "Invalid block index!";
    }

    public static void main(String[] args) {
        try {
            new MemoryAllocationServer();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
