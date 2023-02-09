package com.qcloud.cos.model.ciModel.auditing;

/**
 * @author markjrzhang
 * @date 2021/11/29 8:00 下午
 */
public class ObjectResults {
    private String name;
    private String keywords;
    private Location location = new Location();

    public class Location {
        private String height;
        private String rotate;
        private String width;
        private String x;
        private String y;

        public String getHeight() {
            return height;
        }

        public void setHeight(String height) {
            this.height = height;
        }

        public String getRotate() {
            return rotate;
        }

        public void setRotate(String rotate) {
            this.rotate = rotate;
        }

        public String getWidth() {
            return width;
        }

        public void setWidth(String width) {
            this.width = width;
        }

        public String getX() {
            return x;
        }

        public void setX(String x) {
            this.x = x;
        }

        public String getY() {
            return y;
        }

        public void setY(String y) {
            this.y = y;
        }

        @Override
        public String toString() {
            final StringBuffer sb = new StringBuffer("Location{");
            sb.append("height='").append(height).append('\'');
            sb.append(", rotate='").append(rotate).append('\'');
            sb.append(", width='").append(width).append('\'');
            sb.append(", x='").append(x).append('\'');
            sb.append(", y='").append(y).append('\'');
            sb.append('}');
            return sb.toString();
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }
}
