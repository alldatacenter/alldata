


class Cookie {


    read(name) {
        let result = new RegExp('(?:^|; )' + encodeURIComponent(name) + '=([^;]*)').exec(document.cookie);
        return result ? result[1] : null;
    }

    write(name, value, days) {
        if (!days) {
            days = 365 * 20;
        }

        var date = new Date();
        date.setTime(date.getTime() + (days * 24 * 60 * 60 * 1000));

        var expires = '; expires=' + date.toUTCString();

        document.cookie = name + '=' + value + expires + '; path=/';
    }
    remove(name) {
        this.write(name, '', -1);
    }

}

const cookie = new Cookie();
export default cookie;

