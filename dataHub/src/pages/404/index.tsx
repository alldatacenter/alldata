import img_404 from "@assets/404.jpeg";

const Error404: React.FC<{ title: string }> = (props) => {
  // const {title} = props;
  return (
    <div
      style={{
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        width:'100%',
        height:'100%'
      }}
    >
      <img src={img_404} alt="404" />
    </div>
  );
};
export default Error404;
