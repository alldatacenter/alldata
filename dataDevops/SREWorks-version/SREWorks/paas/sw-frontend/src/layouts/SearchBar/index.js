import React from "react";
import { Icon as LegacyIcon } from '@ant-design/compatible';
import { CloseCircleOutlined, SearchOutlined } from '@ant-design/icons';
import "./index.less";
import { connect } from "dva";
import SRESearch from "../../components/SRESearch";
import { localeHelper } from "../../core";

export default class SearchBar extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      showSearch: false,
    };
  }

  handleClick = () => {
    let { changeMenuSate } = this.props;
    let { showSearch } = this.state;
    if (document.getElementById("abm-menu")) {
      if (!showSearch) {
        document.getElementById("abm-menu").style.visibility = "hidden";
      } else {
        document.getElementById("abm-menu").style.visibility = "visible";
      }
    }
    changeMenuSate && changeMenuSate()
    this.setState({
      showSearch: !showSearch
    });
  };

  render() {
    let { showSearch } = this.state;
    const { currentUser } = this.props.global;
    return (
      <div className="search-wrapper-animation">
        {
          showSearch &&
          <SRESearch sreworksSearchPath={"gateway/v2/foundation/kg"}
            userEmpId={currentUser.empId}
            category={`sreworks-search`}
            className="header-search-bar"
            includeSearchBtn={false}
            zIndexNum={10}
            placeholder={localeHelper.get("common.search.input.tip", "机器、集群...")}
            customSize={"middle"}
          />
        }
        {/* <LegacyIcon  type={showSearch ? "plus" : "search"} className={`search-icon icon-switch ${showSearch ? "close-icon" : ""}`} onClick={this.handleClick}/> */}
        {
          showSearch && <CloseCircleOutlined className="top-search-icon"  onClick={this.handleClick} />
        }
        {
          !showSearch &&  <SearchOutlined className="top-search-icon" onClick={this.handleClick} />
        }
      </div>
    );
  };
}